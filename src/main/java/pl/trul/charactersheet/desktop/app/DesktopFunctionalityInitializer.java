package pl.trul.charactersheet.desktop.app;

import com.github.michal_stempkowski.charactersheet.internal.app.Domain;
import com.github.michal_stempkowski.charactersheet.internal.app.PackageInitializer;
import pl.trul.charactersheet.desktop.gui.GuiDomain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Class responsible for desktop specific domain initialization (gui)
 */
public class DesktopFunctionalityInitializer extends PackageInitializer {
    @Override
    protected List<Supplier<Domain>> getDomainCreators() {
        return new ArrayList<>(Collections.singletonList(
                GuiDomain::new
        ));
    }
}
