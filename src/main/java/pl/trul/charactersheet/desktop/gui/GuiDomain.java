package pl.trul.charactersheet.desktop.gui;

import com.github.michal_stempkowski.charactersheet.internal.Target;
import com.github.michal_stempkowski.charactersheet.internal.app.AppRootLogic;
import com.github.michal_stempkowski.charactersheet.internal.app.Domain;
import com.github.michal_stempkowski.charactersheet.internal.events.Event;
import com.github.michal_stempkowski.charactersheet.internal.events.events.InitializeEvent;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.CyclingTask;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.TaskState;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.events.TaskFinishedEvent;
import com.github.michal_stempkowski.charactersheet.internal.utils.ErrorMonad;
import pl.trul.charactersheet.desktop.DesktopDomainId;

import java.util.logging.Logger;

/**
 * Domain responsible for creation and handling of graphical user interface on desktop instances of application.
 */
public class GuiDomain implements Domain {

    private final ErrorMonad status = new ErrorMonad();
    private final Logger logger;
    private CyclingTask guiMainTask;
    @Override
    public ErrorMonad getStatus() {
        return status;
    }

    public GuiDomain() {
        logger = AppRootLogic.createLogger(Target.DESKTOP, DesktopDomainId.GUI, getClass().getName());
    }

    @Override
    public void setup() {
        guiMainTask = new CyclingTask(
                new MainApp()::mainLoop,
                (TaskState s, ErrorMonad e) -> {
                    if (e.hasErrorOccurred()) {
                        e.get().forEach(Throwable::printStackTrace);
                        return TaskState.ERROR;
                    } else {
                         return TaskState.DONE;
                    }
                });
        AppRootLogic.getEventDispatcher().registerListener(
                InitializeEvent.eventType(),
                this::onInitializeEventStartGui);
    }

    private void onInitializeEventStartGui(Event event) {
        AppRootLogic.getEventDispatcher().registerListener(TaskFinishedEvent.eventType(), this::onTaskFinished);
        AppRootLogic.getTaskScheduler().scheduleTask(guiMainTask);
    }

    private void onTaskFinished(Event event) {
        TaskFinishedEvent ev = Event.tryCast(event);
        if (ev.getTask().id == guiMainTask.id) {
            logger.info("Gui task has finished!");
        }
    }
}
