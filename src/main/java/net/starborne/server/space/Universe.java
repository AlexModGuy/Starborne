package net.starborne.server.space;

import java.util.ArrayList;
import java.util.List;

public class Universe {
    private List<Star> stars = new ArrayList<>();

    public void addStar(Star star) {
        if (!this.stars.contains(star)) {
            this.stars.add(star);
        }
    }
}
