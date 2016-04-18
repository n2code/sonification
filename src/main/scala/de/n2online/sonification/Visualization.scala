package de.n2online.sonification

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.transform.Rotate

class Visualization(val gc: GraphicsContext) {
  val character = new Image(getClass.getResourceAsStream("/agent.png"))

  private def drawCenteredImage(image: Image, center_x: Double, center_y: Double, angle: Double) {
    gc.save
    val matrix: Rotate = new Rotate(Math.toDegrees(angle), center_x, center_y)
    gc.setTransform(matrix.getMxx, matrix.getMyx, matrix.getMxy, matrix.getMyy, matrix.getTx, matrix.getTy)
    val x: Double = center_x - image.getWidth / 2
    val y: Double = center_y - image.getHeight / 2
    gc.drawImage(image, x, y)
    gc.restore
  }

  def paint(agent: Agent, route: Route) {
    val screen: Canvas = gc.getCanvas
    gc.setFill(Color.GREY)
    gc.fillRect(0, 0, screen.getWidth, screen.getHeight)
    var foundNotVisited: Boolean = false
    for (waypoint <- route.getWaypoints) {
      if (waypoint.visited) {
        gc.setFill(Color.DARKGREEN)
      }
      else {
        if (foundNotVisited) {
          gc.setFill(Color.DARKRED)
        }
        else {
          gc.setFill(Color.YELLOW)
          foundNotVisited = true
        }
      }
      gc.fillOval(waypoint.pos.getX - Waypoint.thresholdReached / 2, waypoint.pos.getY - Waypoint.thresholdReached / 2, Waypoint.thresholdReached, Waypoint.thresholdReached)
    }
    drawCenteredImage(character, agent.pos.getX, agent.pos.getY, agent.getOrientation)
  }
}