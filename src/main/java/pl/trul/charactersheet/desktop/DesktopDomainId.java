package pl.trul.charactersheet.desktop;

import com.github.michal_stempkowski.charactersheet.internal.DomainId;

/**
 * Enum used to differentiate between different domains inside desktop target.
 */
public enum DesktopDomainId implements DomainId {
    GUI(0);

    private final int id;

    DesktopDomainId(int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }
}
