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
  private final val agentDiameter = 30
  private final val agentPathDashes = 5.0
  private final val agentPathColor = Color.DARKBLUE
  private final val agentZoomSquareSize = 200
  private final val character = new Image(getClass.getResourceAsStream("/agent.png"))
}

class Visualization(val gc: GraphicsContext,
                    val meshWidth: Double, val meshHeight: Double) {
  var viewport = Rectangle(meshWidth, meshHeight)
  final val scaleProportional = true
  var zoomInOnAgent = false

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

    //fancy view modifiers

    if (zoomInOnAgent) {
      viewport = Rectangle(Visualization.agentZoomSquareSize, Visualization.agentZoomSquareSize,
        agent.pos.getX - Visualization.agentZoomSquareSize/2, agent.pos.getY - Visualization.agentZoomSquareSize/2)
    }

    //adjustment functions

    //stretching scale
    val rawScaleX = screen.getWidth  / viewport.width
    val rawScaleY = screen.getHeight / viewport.height
    //scale proportional if asked for
    val scaleX = if (scaleProportional) math.min(rawScaleX, rawScaleY) else rawScaleX
    val scaleY = if (scaleProportional) math.min(rawScaleX, rawScaleY) else rawScaleY
    //center viewport
    val centerAdjustX = screen.getWidth/2 - viewport.width*scaleX/2
    val centerAdjustY = screen.getHeight/2 - viewport.height*scaleY/2
    //how to calculate projected coordinates on canvas
    val X = (x: Double) => (x - viewport.x) * scaleX + centerAdjustX
    val Y = (y: Double) => (y - viewport.y) * scaleY + centerAdjustY

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
    val propScaledAgent = Visualization.agentDiameter * math.min(scaleX, scaleY)
    drawCenteredImage(Visualization.character, X(agent.pos.getX), Y(agent.pos.getY), agent.getOrientation, Some(propScaledAgent), Some(propScaledAgent))
  }
}