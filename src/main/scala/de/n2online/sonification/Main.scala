package de.n2online.sonification

import javafx.animation.Animation
import javafx.animation.AnimationTimer
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.{Application, Platform}
import javafx.event.{ActionEvent, EventHandler}
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

import de.n2online.sonification.generators.PanningSaws
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.apache.commons.math3.util.FastMath

import scala.util.Random
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

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
  private val sman = new SoundManager
  var agent: Agent = null
  var route: Route = null

  override def stop(): Unit = {
  }

  @throws[Exception]
  def start(stage: Stage) {
    //GUI init

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

    //handling movement and input

    mot = new Motion

    keyboard = new Keyboard
    scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler[KeyEvent] {
      override def handle(evt: KeyEvent): Unit = keyboard.registerKeyDown(evt)
    })
    scene.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler[KeyEvent] {
      override def handle(evt: KeyEvent): Unit = keyboard.registerKeyUp(evt)
    })

    //world data

    agent = new Agent(32, 32, Math.toRadians(45))

    route = new Route
    val mesh = MeshBuilder.getRandomMesh(new Vector2D(0,0), screen.getWidth, screen.getHeight)
    val randomNodes = Random.shuffle(mesh.indices.toList).take(mesh.length/3).map(mesh(_))
    randomNodes.foreach { node => {
      route.addWaypoint(new Waypoint(node))
    } }
    println(s"Random route with ${randomNodes.length} waypoints initialized")

    //sound!

    val generatorReady = sman.setGenerator(new PanningSaws)

    //timers

    //animation:
    val keyframe: KeyFrame = new KeyFrame(Duration.millis(1000 / 25), new EventHandler[ActionEvent] {
      override def handle(t: ActionEvent): Unit = viz.paint(agent, route, mesh)
    })

    val timeline: Timeline = new Timeline(keyframe)
    timeline.setCycleCount(Animation.INDEFINITE)
    timeline.play

    //calculation (sim & sound) :
    val simloop = new AnimationTimer() {

      private var last: Long = 0
      private var deltaSum: Long = 0
      private val recalcThreshold: Long = 10
      private var reducedLogSum: Long = 0

      def handle(tstamp: Long) {
        val now = tstamp / 1000000
        if (last != 0) {
          val delta = now - last
          deltaSum += delta
          reducedLogSum += delta

          if (deltaSum >= recalcThreshold) {
            val partial: Double = deltaSum / 1000.0

            route.currentWaypoint match {
              case Some(target) => {
                mot.handle(partial, keyboard, agent, route)
                if (sman.getGenerator.isDefined) {
                  sman.getGenerator.get.update(target.node.pos.distance(agent.pos))
                }
                if (reducedLogSum > 500) {
                  println("NEXT UP: distance " + target.node.pos.distance(agent.pos) + ", correction " + Math.toDegrees(target.getAngleCorrection(agent)) + "°")
                  reducedLogSum = 0
                }
              }
              case None => {
                route.addWaypoint(new Waypoint(new Node(FastMath.random * screen.getWidth, FastMath.random * screen.getHeight)))
                println("Added new waypoint.")
              }
            }

            deltaSum = 0
          }

        }
        last = now
      }
    }

    generatorReady.onComplete {
      case Success(benchmark) => {
        println(s"Sound generator initialized in $benchmark ms")
        simloop.start()
      }
      case Failure(ex) => println("Sound init failed")
    }
  }

  def log(line: String) {
    val logger: TextArea = scene.lookup("#log").asInstanceOf[TextArea]
    logger.appendText("\n" + line)
  }
}