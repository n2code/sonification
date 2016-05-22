package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

import scala.util.Random

class Experiment(
                  val meshSize: Rectangle,
                  val nodesWanted: Int,
                  val randomSource: Random
                ) {
  val mot = new Motion

  val mesh = MeshBuilder.getRandomMesh(new Vector2D(0,0), meshSize.width, meshSize.height, randomSource)
  private val landmarks = mesh.nodes.toList
  assert(landmarks.length >= nodesWanted + 1, "Grid generator did not produce enough nodes")

  private val randomNodes = randomSource.shuffle(landmarks.indices.toList).take(nodesWanted + 1).map(landmarks(_))
  private val randomRoute = randomNodes.tail.foldLeft(List(randomNodes.head)){
    (l, next) => l ++ Dijkstra.shortestPath(l.last, next, mesh).tail
  }

  val route = new Route(randomRoute.tail.take(nodesWanted).map(new Waypoint(_)))
  Sonification.log(s"[INFO] Random route with ${route.waypoints.length} waypoints initialized")

  val agent = new Agent(randomRoute.head.x, randomRoute.head.y, Math.toRadians(45))
  Sonification.log(s"[INFO] Agent starting position is (${randomRoute.head.x},${randomRoute.head.y})")

}
