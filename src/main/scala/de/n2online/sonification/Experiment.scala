package de.n2online.sonification

import javafx.animation.AnimationTimer
import javafx.scene.chart.{LineChart, XYChart}

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

import scala.util.{Failure, Random, Success, Try}

object Experiment {
  def build(
             worldSize: Rectangle,
             routeLength: Int,
             randomSource: Random,
             anglePlot: LineChart[Number, Number]
           ): Try[Experiment] = {
    try {
      Success(new Experiment(worldSize, routeLength, randomSource, anglePlot))
    } catch {
      case err: Throwable => Failure(err)
    }
  }
}

class Experiment(
                  val meshSize: Rectangle,
                  val nodesWanted: Int,
                  val randomSource: Random,
                  anglePlot: LineChart[Number, Number]
                ) {
  val motion = new Motion
  var simulation: AnimationTimer = null
  val recorder = new PathRecorder(anglePlot)

  val mesh = MeshBuilder.getRandomMesh(new Vector2D(0, 0), meshSize.width, meshSize.height, randomSource) match {
    case Success(world) => world
    case Failure(err) => throw err
  }
  private val landmarks = mesh.nodes.toList
  assert(landmarks.length >= nodesWanted + 1, "Grid generator did not produce enough nodes")

  private val randomNodes = randomSource.shuffle(landmarks.indices.toList).take(nodesWanted + 1).map(landmarks(_))
  private val randomRoute = randomNodes.tail.foldLeft(List(randomNodes.head)) {
    (l, next) => l ++ Dijkstra.shortestPath(l.last, next, mesh).tail
  }

  val route = new Route(randomRoute.tail.take(nodesWanted).map(new Waypoint(_)))
  Sonification.log(s"[INFO] Random route with ${route.waypoints.length} waypoints initialized")

  val agent = new Agent(randomRoute.head.x, randomRoute.head.y, Math.toRadians(45))
  Sonification.log(s"[INFO] Agent starting position is (${randomRoute.head.x},${randomRoute.head.y})")

}
