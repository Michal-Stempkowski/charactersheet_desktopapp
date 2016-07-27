package pl.trul.charactersheet.desktop.gui;

import com.github.michal_stempkowski.charactersheet.internal.Target;
import com.github.michal_stempkowski.charactersheet.internal.app.AppRootLogic;
import com.github.michal_stempkowski.charactersheet.internal.events.events.ShutdownPerformedEvent;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import pl.trul.charactersheet.desktop.DesktopDomainId;

import java.util.logging.Logger;

/**
 * Class responsible for handling main application loop.
 */
public class MainApp extends Application {
    private final Logger logger;

    public MainApp() {
        logger = AppRootLogic.createLogger(Target.DESKTOP, DesktopDomainId.GUI, getClass().getName());
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(VBoxBuilder.create().
                children(new Text("Hi"), new Button("Ok.")).
                alignment(Pos.CENTER).padding(new Insets(5)).build()));
        primaryStage.show();
    }

    public void mainLoop() {
        logger.info("Starting main loop");
        launch();
        logger.info("JavaFx finished");
        AppRootLogic.getEventDispatcher().notifyEvent(new ShutdownPerformedEvent());
    }
}
