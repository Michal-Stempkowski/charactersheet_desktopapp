package pl.trul.charactersheet.desktop.events;

import com.github.michal_stempkowski.charactersheet.internal.events.Event;
import com.github.michal_stempkowski.charactersheet.internal.events.EventBlocker;
import com.github.michal_stempkowski.charactersheet.internal.events.EventConnection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test suite for event dispatcher
 */
public class AsynchronousEventDispatcherTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final int eventType = 0;
    private static final Duration handlingEventLimit = Duration.ofSeconds(10);

    private Event caughtEvent = null;
    private AsynchronousEventDispatcher uut;

    private Consumer<Event> makeCallback() {
        return (event) -> caughtEvent = event;
    }

    private Consumer<Event> makeInfiniteTask() {
        return (event) -> {
            try {
                while (!Thread.interrupted()) {
                    Thread.sleep(10);
                }
            }
            catch (InterruptedException ignored) {

            }
        };
    }

    private Consumer<Event> makeLongTask() {
        return (event) -> {
            try {
                Thread.sleep(1000);
                caughtEvent = event;
            }
            catch (InterruptedException ignored) {

            }
        };
    }

    @Before
    public void setUp() throws Exception {
        uut = new AsynchronousEventDispatcher();
    }

    private void shutdownDispatcher() throws Exception {
        uut.gentleShutdown(Duration.ofSeconds(10), Duration.ofMillis(500));
    }

    @Test
    public void shouldBeAbleToRegisterNewListener() throws Exception {
        // Given:
        uut.registerListener(eventType, makeCallback());
        Event e = new Event(eventType);

        // When:
        uut.notifyEvent(e).block(handlingEventLimit);

        // Then:
        assertThat(caughtEvent, is(equalTo(e)));
        shutdownDispatcher();
    }

    @Test
    public void shouldBeAbleToUnregisterListener() throws Exception {
        // Given:
        EventConnection conn = uut.registerListener(eventType, makeCallback());
        Event e = new Event(eventType);
        uut.unregisterListener(conn);

        // When:
        uut.notifyEvent(e).block(handlingEventLimit);

        // Then:
        assertThat(caughtEvent, is(nullValue()));
        shutdownDispatcher();
    }

    @Test
    public void onUnregisterNotExistingConnectionExceptionIsRaised() throws Exception {
        // Given:
        EventConnection conn = new EventConnection(eventType, makeCallback());
        thrown.expect(NoSuchElementException.class);
        // When/Then:
        uut.unregisterListener(conn);
        shutdownDispatcher();
        assertTrue(true);
    }

    @Test
    public void onUnregisterNotExistingConnectionExceptionIsRaisedCaseTwo() throws Exception {
        // Given:
        uut.registerListener(eventType, makeCallback());
        EventConnection conn = new EventConnection(eventType, makeCallback());
        thrown.expect(NoSuchElementException.class);
        // When/Then:
        uut.unregisterListener(conn);
        shutdownDispatcher();
        assertTrue(true);
    }

    @Test
    public void gentleShutdownShouldAlwaysWork() throws Exception {
        // Given:
        uut.registerListener(eventType, makeInfiniteTask());
        Event e = new Event(eventType);

        // When:
        uut.notifyEvent(e);

        // Then:
        uut.gentleShutdown(Duration.ofMillis(1), Duration.ofSeconds(1));
        assertTrue(true);
    }

    @Test
    public void executionShouldBeAsynchronous() throws Exception {
        // Given:
        uut.registerListener(eventType, makeLongTask());
        Event e = new Event(eventType);

        // When/Then:
        EventBlocker bl = uut.notifyEvent(e);
        assertThat(bl.hasFinished(), is(false));
        assertThat(caughtEvent, is(nullValue()));

        bl.block(handlingEventLimit);
        assertThat(bl.hasFinished(), is(true));
        assertThat(caughtEvent, is(equalTo(e)));

        shutdownDispatcher();
    }
}