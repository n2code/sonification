package de.n2online.sonification;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.HashSet;

public class Keyboard {
    private HashSet<KeyCode> activeCodes;

    public Keyboard() {
        activeCodes = new HashSet<>();
    }

    public void registerKeyDown(KeyEvent event) {
        KeyCode code = event.getCode();
        activeCodes.add(code);
    }

    public void registerKeyUp(KeyEvent event) {
        KeyCode code = event.getCode();
        activeCodes.remove(code);
    }

    public boolean isKeyDown(KeyCode code) {
        return activeCodes.contains(code);
    }
}
