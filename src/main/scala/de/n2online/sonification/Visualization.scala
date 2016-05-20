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

class Visualization(val gc: GraphicsContext,
                    val meshWidth: Double, val meshHeight: Double) {
  var viewport = Rectangle(meshWidth, meshHeight)
  var scaleProportional = true

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

    //adjustment functions
    val rawScaleX = gc.getCanvas.getWidth  / viewport.width
    val rawScaleY = gc.getCanvas.getHeight / viewport.height
    val scaleX = if (scaleProportional) math.min(rawScaleX, rawScaleY) else rawScaleX
    val scaleY = if (scaleProportional) math.min(rawScaleX, rawScaleY) else rawScaleY
    val X = (x: Double) => (x - viewport.x) * scaleX
    val Y = (y: Double) => (y - viewport.y) * scaleY

    //background
    gc.setFill(Visualization.backgroundColor)
    gc.fillRect(0, 0, screen.getWidth, screen.getHeight)

    //node mesh
    gc.setLineDashes(0)
    gc.setStroke(Visualization.meshEdgeColor)
    for (edge <- mesh.edges)
      gc.strokeLine(X(edge.from.x), Y(edge.from.y), X(edge.to.x), Y(edge.to.y))
    gc.setFill(Visualization.meshNodeColor)
    for (node <- mesh.nodes)
      gc.fillOval(X(node.x - Visualization.meshNodeRadius), Y(node.y - Visualization.meshNodeRadius),
        Visualization.meshNodeRadius*2*scaleX, Visualization.meshNodeRadius*2*scaleY)

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
        X(waypoint.node.pos.getX - diameter / 2),
        Y(waypoint.node.pos.getY - diameter / 2),
        diameter*scaleX,
        diameter*scaleY
      )
    }

    //agents path
    val path = agent.recorder.getPath

    gc.setLineDashes(Visualization.agentPathDashes)
    gc.setStroke(Visualization.agentPathColor)
    gc.strokePolyline(path.map(p => X(p.x)).toArray, path.map(p => Y(p.y)).toArray, path.length)

    //agent
    drawCenteredImage(Visualization.character, X(agent.pos.getX), Y(agent.pos.getY), agent.getOrientation, Some(30), Some(30))
  }
}