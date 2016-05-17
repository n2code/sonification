package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

class Node(val x: Double, val y: Double) {
  val pos = new Vector2D(x, y)
  var edges: Set[Edge] = Set()
}
