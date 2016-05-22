package de.n2online.sonification

import javafx.scene.input.{KeyCode, KeyEvent}

class Keyboard {
  private var activeCodes: Set[KeyCode] = Set()
  var consumeEvents = false

  def registerKeyDown(event: KeyEvent) {
    activeCodes = activeCodes + event.getCode
    if (consumeEvents) event.consume()
  }

  def registerKeyUp(event: KeyEvent) {
    activeCodes = activeCodes - event.getCode
    if (consumeEvents) event.consume()
  }

  def isKeyDown(code: KeyCode): Boolean = {
    activeCodes(code)
  }
}