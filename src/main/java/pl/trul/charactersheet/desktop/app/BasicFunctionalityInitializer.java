package pl.trul.charactersheet.desktop.app;


import com.github.michal_stempkowski.charactersheet.internal.app.Domain;
import com.github.michal_stempkowski.charactersheet.internal.app.PackageInitializer;
import com.github.michal_stempkowski.charactersheet.internal.events.EventDomain;
import com.github.michal_stempkowski.charactersheet.internal.logging.LoggingDomain;
import com.github.michal_stempkowski.charactersheet.internal.parallelism.ParallelismDomain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Class responsible for basic domain initialization (events, logging, parallelism)
 */
public class BasicFunctionalityInitializer extends PackageInitializer {
    @Override
    protected List<Supplier<Domain>> getDomainCreators() {
        return new ArrayList<>(Arrays.asList(
                LoggingDomain::new,
                EventDomain::new,
                ParallelismDomain::new
        ));
    }
}
