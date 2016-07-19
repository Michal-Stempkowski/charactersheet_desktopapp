package pl.trul.charactersheet.desktop;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class SanityTest {
    @Test
    public void performSanityCheck() {
        assertThat(true, is(equalTo(true)));
    }
}
