package de.n2online.sonification

case class Rectangle(width: Double, height: Double, x: Double = 0, y: Double = 0) {
  require(width > 0 && height > 0)

  override def toString: String = s"${width}x${height}" + (if ((x, y) ==(0, 0)) "" else s"($x,$y)")
}
