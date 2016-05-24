package de.n2online.sonification

import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.transform.Rotate

object Visualization {
  private final val meshNodeRadius = 3
  private final val meshNodeColor = Color.GREY
  private final val meshEdgeColor = Color.LIGHTGREY
  private final val backgroundColor = Color.WHITE
  private final val standbyColor = Color.GREY
  private final val agentPathDashes = 5.0
  private final val agentPathColor = Color.DARKBLUE
  private final val agentZoomSquareSize = 200
  private final val character = new Image(getClass.getResourceAsStream("/agent.png"))
  private final val characterBoundaryTolerance = 3
  final val FPS = 30
}

class Visualization(val gc: GraphicsContext, val worldSize: Rectangle) {
  final val scaleProportional = true
  var zoomInOnAgent = false
  var rotateWithAgent = false

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

  def paintStandbyScreen() {
    val screen: Canvas = gc.getCanvas
    gc.setFill(Visualization.standbyColor)
    gc.fillRect(0, 0, screen.getWidth, screen.getHeight)
  }

  def paint(experiment: Experiment) {
    val screen: Canvas = gc.getCanvas
    val agent = experiment.agent
    val route = experiment.route
    val mesh = experiment.mesh
    val recorder = experiment.recorder

    /***** fancy view modifiers *****/

    val viewport = zoomInOnAgent match {
      case false => Rectangle(worldSize.width, worldSize.height)
      case true => Rectangle(Visualization.agentZoomSquareSize, Visualization.agentZoomSquareSize,
        agent.pos.getX - Visualization.agentZoomSquareSize / 2, agent.pos.getY - Visualization.agentZoomSquareSize / 2)
    }

    /***** adjustment functions *****/

    //stretching scale
    val rawScaleX = screen.getWidth / viewport.width
    val rawScaleY = screen.getHeight / viewport.height
    //scale proportional if asked for
    val scaleX = if (scaleProportional) math.min(rawScaleX, rawScaleY) else rawScaleX
    val scaleY = if (scaleProportional) math.min(rawScaleX, rawScaleY) else rawScaleY
    //center viewport
    val centerAdjustX = screen.getWidth / 2 - viewport.width * scaleX / 2
    val centerAdjustY = screen.getHeight / 2 - viewport.height * scaleY / 2
    //how to calculate projected coordinates on canvas
    val X = (x: Double) => (x - viewport.x) * scaleX + centerAdjustX
    val Y = (y: Double) => (y - viewport.y) * scaleY + centerAdjustY

    /***** DRAWING ****/

    //calculating rotated screen rectangle as maximum drawing scope
    val hypo = Math.sqrt(Math.pow(screen.getWidth, 2) * Math.pow(screen.getHeight, 2))
    val performanceRect = Rectangle(2 * hypo , 2 * hypo, screen.getWidth / 2 - hypo, screen.getHeight / 2 - hypo)

    //background
    gc.setFill(Visualization.backgroundColor)
    gc.fillRect(0, 0, screen.getWidth, screen.getHeight)

    //agent view
    if (rotateWithAgent && zoomInOnAgent) {
      gc.save()
      val matrix: Rotate = new Rotate(Math.toDegrees(-agent.getOrientation - math.Pi / 2), screen.getWidth / 2, screen.getHeight / 2)
      gc.setTransform(matrix.getMxx, matrix.getMyx, matrix.getMxy, matrix.getMyy, matrix.getTx, matrix.getTy)
      //restored later
    }

    //node mesh (performance optimized)
    gc.setLineDashes(0)
    gc.setStroke(Visualization.meshEdgeColor)
    for (edge <- mesh.edges) {
      val a_x = X(edge.from.x)
      val a_y = Y(edge.from.y)
      val b_x = X(edge.to.x)
      val b_y = Y(edge.to.y)
      if (performanceRect.overlaps(Rectangle(b_x - a_x, b_y - a_y, a_x, a_y))) {
        gc.strokeLine(a_x, a_y, b_x, b_y)
      }
    }
    gc.setFill(Visualization.meshNodeColor)
    for (node <- mesh.nodes) {
      val center_x = X(node.x - Visualization.meshNodeRadius)
      val center_y = Y(node.y - Visualization.meshNodeRadius)
      val p_x = Visualization.meshNodeRadius * 2 * scaleX
      val p_y = Visualization.meshNodeRadius * 2 * scaleY
      if (performanceRect.overlaps(Rectangle(p_x, p_y, center_x - p_x / 2, center_y - p_y / 2))) {
        gc.fillOval(center_x, center_y, p_x - 2, p_y - 2)
      }
    }

    //waypoints (performance optimized)
    val drawWaypoint = (x: Double, y: Double, radius: Double, color: Color) => {
      val center_x = X(x - radius)
      val center_y = Y(y - radius)
      val p_x = radius * 2 * scaleX
      val p_y = radius * 2 * scaleY
      if (performanceRect.overlaps(Rectangle(p_x, p_y, center_x - p_x / 2, center_y - p_y / 2))) {
        gc.setFill(color)
        gc.fillOval(center_x, center_y, p_x, p_y)
      }
    }

    for (waypoint <- route.waypoints) {
      //outer ring to indicate hitbox
      if (waypoint == route.currentWaypoint.orNull) {
        drawWaypoint(waypoint.node.pos.getX, waypoint.node.pos.getY, Waypoint.thresholdReached, Waypoint.colorCurrentHitbox)
      }
      //regular mash overlay
      val color =
        if (waypoint.visited) Waypoint.colorVisited
        else if (waypoint == route.currentWaypoint.orNull) Waypoint.colorCurrentCore
        else if (waypoint == route.nextWaypoint.orNull) Waypoint.colorNext
        else Waypoint.colorLater
      drawWaypoint(waypoint.node.pos.getX, waypoint.node.pos.getY, Visualization.meshNodeRadius, color)
    }

    //agents path
    val path = recorder.getPath

    gc.setLineDashes(Visualization.agentPathDashes)
    gc.setStroke(Visualization.agentPathColor)
    gc.strokePolyline(path.map(p => X(p.x)).toArray, path.map(p => Y(p.y)).toArray, path.length)

    //agent
    val characterImageRadius = Waypoint.thresholdReached - Visualization.meshNodeRadius + Visualization.characterBoundaryTolerance
    val propScaledCharacter = 2 * characterImageRadius * math.min(scaleX, scaleY)
    drawCenteredImage(Visualization.character,
      X(agent.pos.getX), Y(agent.pos.getY),
      if (rotateWithAgent && zoomInOnAgent) -math.Pi / 2 else agent.getOrientation,
      Some(propScaledCharacter), Some(propScaledCharacter))

    //reverse agent view
    if (rotateWithAgent && zoomInOnAgent) {
      gc.restore()
    }
  }
}