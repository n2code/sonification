package de.n2online.sonification

import de.sciss.synth._

abstract class Generator {
  def initialize(server: Server)
  def update(absoluteDistance: Double)
}
