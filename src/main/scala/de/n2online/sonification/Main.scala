package de.n2online.sonification

import javafx.animation.Animation
import javafx.animation.AnimationTimer
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.event.EventHandler
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

object Sonification {
  def main(args: Array[String]) {
    Application.launch(classOf[Main], args: _*)
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
    //TODO
    //scene.addEventFilter(KeyEvent.KEY_PRESSED, (evt: KeyEvent) => keyboard.registerKeyDown(_))
    //scene.addEventFilter(KeyEvent.KEY_RELEASED, (evt: KeyEvent) => keyboard.registerKeyUp(_))

    agent = new Agent(32, 32, Math.toRadians(45))

    route = new Route
    1 to 5 foreach { _ => route.addWaypoint(new Waypoint(FastMath.random * screen.getWidth, FastMath.random * screen.getHeight)) }

    //TODO
    //val keyframe: KeyFrame = new KeyFrame(Duration.millis(1000 / 25), viz.paint(agent, route))
    //val timeline: Timeline = new Timeline(keyframe)
    //timeline.setCycleCount(Animation.INDEFINITE)
    //timeline.play
    val physics: AnimationTimer = new AnimationTimer() {
      private var last: Long = 0
      private
      var deltaSum: Long = 0
      final
      private val minDelta: Long = 10
      def handle(tstamp: Long) {
        val now = tstamp / 1000000
        if (last != 0) {
          deltaSum += now - last
          if (deltaSum >= minDelta) {
            val partial: Double = deltaSum / 1000.0
            if (route.getWaypoints.isEmpty) {
              route.addWaypoint(new Waypoint(FastMath.random * screen.getWidth, FastMath.random * screen.getHeight))
            }
            mot.handle(partial, keyboard, agent, route)
            val target: Waypoint = route.currentWaypoint
            println("NEXT UP: distance " + target.pos.distance(agent.pos) + ", correction " + Math.toDegrees(target.getAngleCorrection(agent)) + "°")
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