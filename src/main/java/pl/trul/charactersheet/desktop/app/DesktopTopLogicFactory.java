package pl.trul.charactersheet.desktop.app;

import com.github.michal_stempkowski.charactersheet.internal.DomainId;
import com.github.michal_stempkowski.charactersheet.internal.Target;
import com.github.michal_stempkowski.charactersheet.internal.app.AppRootLogic;
import com.github.michal_stempkowski.charactersheet.internal.app.PackageInitializer;
import com.github.michal_stempkowski.charactersheet.internal.app.TopLogicFactory;
import com.github.michal_stempkowski.charactersheet.internal.events.Event;
import com.github.michal_stempkowski.charactersheet.internal.events.EventDispatcher;
import com.github.michal_stempkowski.charactersheet.internal.events.events.InitializeEvent;
import com.github.michal_stempkowski.charactersheet.internal.events.events.ShutdownPerformedEvent;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.TaskScheduler;
import pl.trul.charactersheet.desktop.events.AsynchronousEventDispatcher;
import pl.trul.charactersheet.desktop.parallelism.AsynchronousTaskScheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Class responsible for providing desktop
 */
public class DesktopTopLogicFactory implements TopLogicFactory {
    private static final String LINE_FORMATTING = "%s|\t%s|\t%s";
    private EventDispatcher eventDispatcher;
    private TaskScheduler taskScheduler;
    private final Lock factoryLock = new ReentrantLock();
    private final Condition shutdownPerformedCondition = factoryLock.newCondition();
    private boolean isRunning = false;

    @Override
    public List<PackageInitializer> getPackageInitializers() {
        return new ArrayList<>(Arrays.asList(
                new BasicFunctionalityInitializer(),
                new DesktopFunctionalityInitializer()
        ));
    }

    @Override
    public EventDispatcher getEventDispatcher() {
        if (eventDispatcher == null) {
            eventDispatcher = new AsynchronousEventDispatcher();
        }
        return eventDispatcher;
    }

    @Override
    public Logger createLogger(Target targetId, DomainId domainId, String name) {
        return Logger.getLogger(String.format(LINE_FORMATTING, targetId.toString(), domainId.toString(), name));
    }

    @Override
    public void start() {
        getEventDispatcher().registerListener(ShutdownPerformedEvent.eventType(), this::on_shutdown_performed);
//        runDemoThread();
        AppRootLogic.getEventDispatcher().notifyEvent(new InitializeEvent());
        sleepUntilShutdownPerformed();
        performGentleShutdown();
    }

    @Override
    public TaskScheduler getTaskScheduler() {
        if (taskScheduler == null) {
            taskScheduler = new AsynchronousTaskScheduler();
        }
        return taskScheduler;
    }

    private void performGentleShutdown() {
        Duration finalizationTime = Duration.ofSeconds(1);
        Duration lastResortTime = Duration.ofSeconds(1);
        AppRootLogic.getEventDispatcher().gentleShutdown(finalizationTime, lastResortTime);
    }

    private void sleepUntilShutdownPerformed() {
        factoryLock.lock();
        try {
            isRunning = true;
            while (isRunning) {
                shutdownPerformedCondition.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            factoryLock.unlock();
        }
    }

    private void runDemoThread() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(3_000);
                getEventDispatcher().notifyEvent(new ShutdownPerformedEvent());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.start();
    }

    private void on_shutdown_performed(Event event) {
        factoryLock.lock();
        try {
            isRunning = false;
            shutdownPerformedCondition.signalAll();
        } finally {
            factoryLock.unlock();
        }
    }
}
