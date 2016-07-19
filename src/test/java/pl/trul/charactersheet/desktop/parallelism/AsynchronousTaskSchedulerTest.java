package pl.trul.charactersheet.desktop.parallelism;

import com.github.michal_stempkowski.charactersheet.internal.app.AppRootLogic;
import com.github.michal_stempkowski.charactersheet.internal.events.Event;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.CyclingTask;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.TaskState;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.events.TaskFinishedEvent;
import com.github.michal_stempkowski.charactersheet.internal.utils.ErrorMonad;
import org.junit.Before;
import org.junit.Test;
import pl.trul.charactersheet.desktop.app.DesktopTopLogicFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class AsynchronousTaskSchedulerTest {

    private AsynchronousTaskScheduler uut;
    private ReentrantLock lock;
    private Condition taskDoneCondition;
    private CyclingTask taskSignallingWhenItIsDone;
    private boolean taskDone;

    private void onTaskDoneCallback(Event event) {
        TaskFinishedEvent e = Event.tryCast(event);
        if (e.getTask().id == taskSignallingWhenItIsDone.id) {
            taskDone = true;
        }
    }

    @Before
    public void setUp() throws Exception {
        AppRootLogic root = new AppRootLogic(new DesktopTopLogicFactory());
        uut = new AsynchronousTaskScheduler();
        lock = new ReentrantLock();
        setUpTaskSignallingWhenItIsDone();
    }

    private void setUpTaskSignallingWhenItIsDone() {
        taskDoneCondition = lock.newCondition();
        taskSignallingWhenItIsDone = new CyclingTask(() -> {
            lock.lock();
            taskDoneCondition.signalAll();
            lock.unlock();
        },
        (TaskState state, ErrorMonad error) -> TaskState.DONE);
        taskDone = false;
        AppRootLogic.getEventDispatcher().registerListener(
                TaskFinishedEvent.eventType(), this::onTaskDoneCallback);
    }

    @Test
    public void shouldBeAbleToAddNewTask() {
        //Given:
        CyclingTask task = new CyclingTask(() -> {
            throw new Exception();
        },
        (TaskState state, ErrorMonad error) -> TaskState.ERROR);
        // When:
        uut.scheduleTask(task);
        // Then:
        assertThat(uut.tasksInQueue(), is(equalTo(1)));
        assertThat(task.getState(), is(equalTo(TaskState.CREATED)));
    }

    @Test
    public void initShouldCauseTaskExecution() throws InterruptedException {
        //Given:
        uut.scheduleTask(taskSignallingWhenItIsDone);
        // When:
        uut.init();
        // Then:
        expectTaskDone();
    }

    @Test
    public void ifInitEarlierAddingNewTaskShouldCauseExecution() throws InterruptedException {
        //Given:
        uut.init();
        // When:
        uut.scheduleTask(taskSignallingWhenItIsDone);
        // Then:
        expectTaskDone();
    }

    private void expectTaskDone() throws InterruptedException {
        lock.lock();
        taskDoneCondition.await(3, TimeUnit.SECONDS);
        Thread.sleep(500);
        assertThat(taskSignallingWhenItIsDone.getState(), is(equalTo(TaskState.DONE)));
        assertThat(taskDone, is(true));
    }
}