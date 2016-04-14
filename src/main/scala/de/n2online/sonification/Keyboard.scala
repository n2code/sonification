package de.n2online.sonification

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import java.util.HashSet

class Keyboard {
  activeCodes = new util.HashSet[KeyCode]
  private var activeCodes: util.HashSet[KeyCode] = null

  def registerKeyDown(event: KeyEvent) {
    val code: KeyCode = event.getCode
    activeCodes.add(code)
  }

  def registerKeyUp(event: KeyEvent) {
    val code: KeyCode = event.getCode
    activeCodes.remove(code)
  }

  def isKeyDown(code: KeyCode): Boolean = {
    return activeCodes.contains(code)
  }
}