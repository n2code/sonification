package de.n2online.sonification

case class Graph(V: Set[Node], E: Set[Edge]) {
  val nodes = V
  val edges = E
}
