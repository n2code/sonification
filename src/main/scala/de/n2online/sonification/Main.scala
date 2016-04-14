package de.n2online.sonification

import javafx.animation.Animation
import javafx.animation.AnimationTimer
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.TextArea
import javafx.scene.input.KeyEvent
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import javafx.util.Duration
import org.apache.commons.math3.util.FastMath

object Main {
  def main(args: Array[String]) {
    launch(args)
  }
}

class Main extends Application {
  private var root: Parent = null
  private var scene: Scene = null
  private var screen: Canvas = null
  private var gc: GraphicsContext = null
  private var keyboard: Keyboard = null
  private var viz: Visualization = null
  private var mot: Motion = null
  var agent: Agent = null
  var route: Route = null

  @throws[Exception]
  def start(stage: Stage) {
    root = FXMLLoader.load(getClass.getResource("/Main.fxml"))
    stage.setTitle("Sonification")
    scene = new Scene(root, 800, 600)
    stage.setScene(scene)
    stage.show
    val monitor: AnchorPane = scene.lookup("#monitor").asInstanceOf[AnchorPane]
    screen = scene.lookup("#screen").asInstanceOf[Canvas]
    screen.widthProperty.bind(monitor.widthProperty)
    screen.heightProperty.bind(monitor.heightProperty)
    gc = screen.getGraphicsContext2D
    viz = new Visualization(gc)
    mot = new Motion
    keyboard = new Keyboard
    scene.addEventFilter(KeyEvent.KEY_PRESSED, evt -> keyboard.registerKeyDown(evt))
    scene.addEventFilter(KeyEvent.KEY_RELEASED, evt -> keyboard.registerKeyUp(evt))
    agent = new Agent(32, 32, Math.toRadians(45))
    route = new Route
    var i: Int = 0
    while (i < 5) {
      {
        route.addWaypoint(new Waypoint(FastMath.random * screen.getWidth, FastMath.random * screen.getHeight))
      }
      ({
        i += 1; i - 1
      })
    }
    val timeline: Timeline = new Timeline(new KeyFrame(Duration.millis(1000 / 25), evt -> viz.paint(agent, route)))
    timeline.setCycleCount(Animation.INDEFINITE)
    timeline.play
    val physics: AnimationTimer = new AnimationTimer() {
      private var last: Long = 0
      private
      var deltaSum: Long = 0
      final
      private val minDelta: Long = 10
      def handle(now: Long) {
        now /= 1000000
        if (last != 0) {
          deltaSum += now - last
          if (deltaSum >= minDelta) {
            val partial: Double = deltaSum / 1000.0
            mot.handle(partial, keyboard, agent, route)
            val target: Waypoint = route.currentWaypoint
            if (target != null) {
              System.out.println("NEXT UP: distance " + target.pos.distance(agent.pos) + ", correction " + Math.toDegrees(target.getAngleCorrection(agent)) + "°")
            }
            else {
              route.addWaypoint(new Waypoint(FastMath.random * screen.getWidth, FastMath.random * screen.getHeight))
            }
            deltaSum = 0
          }
        }
        last = now
      }
    }
    physics.start
  }

  def log(line: String) {
    val logger: TextArea = scene.lookup("#log").asInstanceOf[TextArea]
    logger.appendText("\n" + line)
  }
}