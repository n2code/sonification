package de.n2online.sonification

case class Rectangle(width: Double, height: Double, x: Double = 0, y: Double = 0) {
  require(width > 0 && height > 0)
}
