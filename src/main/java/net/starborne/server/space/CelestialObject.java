package net.starborne.server.space;

import java.util.ArrayList;
import java.util.List;

public class CelestialObject<ORB extends CelestialObject> {
    private ORB orbiting;
    private List<CelestialObject<?>> children = new ArrayList<>();

    public void orbit(ORB orbiting) {
        ORB prevOrbiting = this.orbiting;
        if (prevOrbiting != null) {
            prevOrbiting.removeChild(this);
        }
        this.orbiting = orbiting;
        if (this.orbiting != null) {
            this.orbiting.addChild(this);
        }
    }

    protected void addChild(CelestialObject<?> celestialObject) {
        if (!this.children.contains(celestialObject)) {
            this.children.add(celestialObject);
        }
    }

    protected void removeChild(CelestialObject<?> celestialObject) {
        this.children.remove(celestialObject);
    }

    public List<CelestialObject<?>> getChildren() {
        return this.children;
    }

    public ORB getOrbiting() {
        return this.orbiting;
    }
}
