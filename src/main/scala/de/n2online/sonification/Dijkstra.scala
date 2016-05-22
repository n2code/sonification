package de.n2online.sonification

import scala.util.control.Breaks._

object Dijkstra {
  def shortestPath(start: Node, target: Node, graph: Graph): List[Node] = {

    //preparation and helpers

    val nodes: Set[DijkstraNode] = graph.nodes.map(n => new DijkstraNode(n, if (n == start) Some(0) else None))
    val edges = graph.edges

    val queue = () => {
      nodes.filter(_.queued)
    }

    def updateDistance(u: DijkstraNode, v: DijkstraNode) = {
      val alternative = u.distance + u.node.pos.distance(v.node.pos)
      if (alternative < v.distance) {
        v.distance = alternative
        v.predecessor = Some(u)
      }
    }

    //running the algorithm

    breakable {
      while (queue().nonEmpty) {
        val u = queue().minBy(_.distance)
        u.queued = false
        if (u.node == target) break
        val neighboursInQueue = nodes.filter { v => edges.contains(Edge(u.node, v.node)) && queue().contains(v) }
        neighboursInQueue.foreach(v => updateDistance(u, v))
      }
    }

    //piecing together the shortest path

    def calculatePath(target: DijkstraNode): List[DijkstraNode] = target.predecessor.isDefined match {
      case true => calculatePath(target.predecessor.get) ++ List(target)
      case false => List(target)
    }

    val targetDNode = nodes.find(_.node == target)
    targetDNode.isDefined match {
      case true => calculatePath(targetDNode.get).map(_.node)
      case false => throw new IllegalArgumentException("Dijkstra did not find a path")
    }

  }
}

class DijkstraNode(n: Node, dist: Option[Double]) {
  val node = n
  var queued = true
  var distance = dist.getOrElse(Double.PositiveInfinity)
  var predecessor: Option[DijkstraNode] = None

  override def equals(that: Any): Boolean = that match {
    case that: DijkstraNode => that.node.equals(this.node)
    case _ => false
  }

  override def hashCode = node.hashCode()
}
