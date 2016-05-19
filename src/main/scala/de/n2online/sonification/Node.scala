package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

case class Node(val x: Double, val y: Double, cell: Cell) {
  def pos = new Vector2D(x, y)
}
