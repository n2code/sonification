package de.n2online.sonification;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class Waypoint {
    public Vector2D pos;
    public static final double thresholdReached = 30;
    public boolean visited = false;

    Waypoint(double x, double y) {
        pos = new Vector2D(x, y);
    }

    public boolean isReached(Vector2D compare) {
        if (compare.distance(pos) <= thresholdReached) {
            return true;
        } else {
            return false;
        }
    }
}
