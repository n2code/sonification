package de.n2online.sonification

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

class Keyboard {
  private var activeCodes: Set[KeyCode] = null

  def registerKeyDown(event: KeyEvent) {
    activeCodes = activeCodes + event.getCode
  }

  def registerKeyUp(event: KeyEvent) {
    activeCodes = activeCodes - event.getCode
  }

  def isKeyDown(code: KeyCode): Boolean = {
    activeCodes(code)
  }
}