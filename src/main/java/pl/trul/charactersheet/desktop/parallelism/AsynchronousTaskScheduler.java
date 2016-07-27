package pl.trul.charactersheet.desktop.parallelism;


import com.github.michal_stempkowski.charactersheet.internal.InternalDomainId;
import com.github.michal_stempkowski.charactersheet.internal.Target;
import com.github.michal_stempkowski.charactersheet.internal.app.AppRootLogic;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.CyclingTask;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.TaskScheduler;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.events.TaskFinishedEvent;

import java.rmi.server.UID;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    private Logger logger = AppRootLogic.createLogger(Target.DESKTOP, InternalDomainId.PARALLELISM, getClass().getName());

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

    @Override
    public void gentleShutdown(Duration finalizationTime, Duration lastResortTime) {
        logger.info("Gentle shutdown has begun");
        executors.shutdown();
        if (!safelyAwaitTermination(finalizationTime)) {
            logger.warning("Last resort shutdown has begun!");
            executors.shutdownNow();
            if (!safelyAwaitTermination(lastResortTime)) {
                logger.severe("Last resort failed, performing emergency shutdown!!!");
                System.exit(1);
            }
        }
        logger.info("Shutdown performed gracefully");
    }

    private boolean safelyAwaitTermination(Duration terminationLimit) {
        try {
            return executors.awaitTermination(terminationLimit.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            logger.severe("awaitTermination during gentleShutdown has been terminated!");
            return false;
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
