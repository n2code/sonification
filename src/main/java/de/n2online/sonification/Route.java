package de.n2online.sonification;

import java.util.LinkedList;
import java.util.List;

public class Route {
    private LinkedList<Waypoint> waypoints;

    Route() {
        waypoints = new LinkedList<>();
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public Waypoint currentWaypoint() {
        for (int i = 0; i < waypoints.size(); i++) {
            if (!waypoints.get(i).visited) {
                return waypoints.get(i);
            }
        }
        return null;
    }
}
