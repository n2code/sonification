package de.n2online.sonification

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.transform.Rotate

object Visualization {
  private final val meshNodeRadius = 5
  private final val meshNodeColor = Color.GREY
  private final val meshEdgeColor = Color.LIGHTGREY
  private final val backgroundColor = Color.WHITE
  private final val agentPathDashes = 5.0
  private final val agentPathColor = Color.DARKBLUE
  private final val character = new Image(getClass.getResourceAsStream("/agent.png"))
}

class Visualization(val gc: GraphicsContext) {

  private def drawCenteredImage(
                                 image: Image,
                                 center_x: Double, center_y: Double,
                                 angle: Double,
                                 w: Option[Double] = None, h: Option[Double] = None
                               ) {
    gc.save()
    val width = w.getOrElse(h.getOrElse(image.getWidth))
    val height = h.getOrElse(w.getOrElse(image.getHeight))
    val matrix: Rotate = new Rotate(Math.toDegrees(angle), center_x, center_y)
    gc.setTransform(matrix.getMxx, matrix.getMyx, matrix.getMxy, matrix.getMyy, matrix.getTx, matrix.getTy)
    val x: Double = center_x - width / 2
    val y: Double = center_y - height / 2
    gc.drawImage(image, x, y, width, height)
    gc.restore()
  }

  def paint(agent: Agent, route: Route, mesh: Graph) {
    val screen: Canvas = gc.getCanvas

    //background
    gc.setFill(Visualization.backgroundColor)
    gc.fillRect(10, 10, screen.getWidth-20, screen.getHeight-20)

    //node mesh
    gc.setLineDashes(0)
    gc.setStroke(Visualization.meshEdgeColor)
    for (edge <- mesh.edges)
      gc.strokeLine(edge.from.x, edge.from.y, edge.to.x, edge.to.y)
    gc.setFill(Visualization.meshNodeColor)
    for (node <- mesh.nodes)
      gc.fillOval(node.x - Visualization.meshNodeRadius, node.y - Visualization.meshNodeRadius,
        Visualization.meshNodeRadius*2, Visualization.meshNodeRadius*2)

    //waypoints
    for (waypoint <- route.waypoints) {
      val color =
        if (waypoint.visited) Waypoint.colorVisited
        else if (waypoint == route.currentWaypoint.orNull) Waypoint.colorCurrent
        else if (waypoint == route.nextWaypoint.orNull) Waypoint.colorNext
        else Waypoint.colorLater
      gc.setFill(color)
      val diameter =
        if (waypoint == route.currentWaypoint.orNull) Waypoint.thresholdReached
        else Visualization.meshNodeRadius*2
      gc.fillOval(
        waypoint.node.pos.getX - diameter / 2,
        waypoint.node.pos.getY - diameter / 2,
        diameter,
        diameter
      )
    }

    //agents path
    val path = agent.recorder.getPath

    gc.setLineDashes(Visualization.agentPathDashes)
    gc.setStroke(Visualization.agentPathColor)
    gc.strokePolyline(path.map(_.x).toArray, path.map(_.y).toArray, path.length)

    //agent
    drawCenteredImage(Visualization.character, agent.pos.getX, agent.pos.getY, agent.getOrientation, Some(30), Some(30))
  }
}