package pl.trul.charactersheet.desktop.parallelism;


import com.github.michal_stempkowski.charactersheet.internal.DomainId;
import com.github.michal_stempkowski.charactersheet.internal.Target;
import com.github.michal_stempkowski.charactersheet.internal.app.AppRootLogic;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.CyclingTask;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.TaskScheduler;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.events.TaskFinishedEvent;

import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Class responsible for scheduling and running long running tasks. It uses thread
 * pool in order to make parallel task execution.
 */
public class AsynchronousTaskScheduler implements TaskScheduler {
    private ExecutorService executors = Executors.newCachedThreadPool();
    private Map<UID, CyclingTask> tasks = new HashMap<>();
    private boolean hasStarted = false;
    private final Object mutex = new Object();
    private Logger logger = AppRootLogic.createLogger(Target.DESKTOP, DomainId.PARALLELISM, getClass().getName());

    @Override
    public void scheduleTask(CyclingTask cyclingTask) {
        synchronized (mutex) {
            logger.info(String.format("Scheduling task [%s]", cyclingTask.id.toString()));
            tasks.put(cyclingTask.id, cyclingTask);
            if (hasStarted) {
                logger.fine(String.format("Submitting task [%s]", cyclingTask.id.toString()));
                executors.submit(() -> mainTaskLoop(cyclingTask));
            }
        }
    }

    @Override
    public void init() {
        synchronized (mutex) {
            logger.info("Initialization");
            tasks.forEach((UID id, CyclingTask task) -> {
                logger.fine(String.format("Submitting task [%s]", task.id.toString()));
                executors.submit(() -> mainTaskLoop(task));
            });
            hasStarted = true;
        }
    }

    @Override
    public int tasksInQueue() {
        synchronized (mutex) {
            int result = tasks.size();
            logger.fine(String.format("Checking tasks in scheduler, currently %d registered", result));
            return result;
        }
    }

    private void mainTaskLoop(CyclingTask task) {
        logger.info(String.format("Starting task %s", task.id.toString()));
        int cycles = 0;
        while (!task.getState().hasFinished()) {
            task.runSingleCycle();
            logger.fine(String.format("Task has finished cycle %s", cycles++));
            task.evaluateTask();
        }

        logger.info(String.format("Finalizing task %s", task.id.toString()));
        AppRootLogic.getEventDispatcher().notifyEvent(new TaskFinishedEvent(task));
        synchronized (mutex) {
            tasks.remove(task.id);
            logger.fine(String.format("Task cleanup finished: %s", task.id.toString()));
        }
    }
}
