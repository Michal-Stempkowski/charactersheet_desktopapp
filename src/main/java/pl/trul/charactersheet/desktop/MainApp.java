package pl.trul.charactersheet.desktop;

import com.github.michal_stempkowski.charactersheet.internal.app.AppRootLogic;
import pl.trul.charactersheet.desktop.app.DesktopTopLogicFactory;

/**
 * Class responsible for starting main application for Desktop release.
 */
public class MainApp {
    public static void main(String[] args) {
        AppRootLogic rootLogic = new AppRootLogic(new DesktopTopLogicFactory());
        if (AppRootLogic.init()) {
            AppRootLogic.start();
        }
        System.out.println("\n\n\n\nMainApp started and done:");
    }
}
