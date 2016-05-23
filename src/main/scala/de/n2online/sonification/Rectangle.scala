package de.n2online.sonification

case class Rectangle(width: Double, height: Double, x: Double = 0, y: Double = 0) {
  //require(width > 0 && height > 0)

  override def toString: String = s"${width}x$height" + (if ((x, y) ==(0, 0)) "" else s"($x,$y)")

  def getCorners = {
    for (x <- Array(x, x + width); y <- Array(y, y + height)) yield (x, y)
  }

  def overlaps(other: Rectangle) = {
    other.getCorners.exists(c => (c._1 >= x) && (c._1 <= x + width) && (c._2 >= y) && (c._2 <= y + height))
  }
}
