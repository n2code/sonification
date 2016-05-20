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
import javafx.scene.layout.Pane
import javafx.stage.Stage
import javafx.util.Duration

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

import scala.util.Random
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object Sonification {
  var mainref: Main = null

  def main(args: Array[String]) {
    Application.launch(classOf[Main], args: _*)
  }

  def log(text: String): Unit = {
    if (mainref != null) mainref.log(text)
    else println(s"=> $text")
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
    sman.stopServer()
  }

  @throws[Exception]
  def start(stage: Stage) {
    //GUI init

    root = FXMLLoader.load(getClass.getResource("/Main.fxml"))
    stage.setTitle("Sonification")
    scene = new Scene(root, 800, 600)
    stage.setScene(scene)
    stage.show
    Sonification.mainref = this

    val monitor: Pane = scene.lookup("#monitor").asInstanceOf[Pane]
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

    val mesh = MeshBuilder.getRandomMesh(new Vector2D(0,0), monitor.getWidth, monitor.getHeight)
    val landmarks = mesh.nodes.toList
    assert(landmarks.length >= 2, "at least two nodes for route required!")

    val randomNodes = Random.shuffle(landmarks.indices.toList).take(5).map(landmarks(_))
    val randomRoute = randomNodes.tail.foldLeft(List(randomNodes.head)){
      (l, next) => l ++ Dijkstra.shortestPath(l.last, next, mesh).tail
    }

    route = new Route(randomRoute.map(new Waypoint(_)))
    Sonification.log(s"Random route with ${randomNodes.length} waypoints initialized")

    agent = new Agent(randomRoute.head.x, randomRoute.head.y, Math.toRadians(45))

    //sound!

    val generatorReady = sman.setGenerator(new generators.PanningScale)

    //timers

    //animation:
    val keyframe: KeyFrame = new KeyFrame(Duration.millis(1000 / 25), new EventHandler[ActionEvent] {
      override def handle(t: ActionEvent): Unit = viz.paint(agent, route, mesh)
    })

    val timeline: Timeline = new Timeline(keyframe)
    timeline.setCycleCount(Animation.INDEFINITE)
    timeline.play()

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
                  val dist = target.node.pos.distance(agent.pos)
                  val angle = target.getAngleCorrection(agent)
                  sman.getGenerator.get.update(dist, angle)
                }
                if (reducedLogSum > 500) {
                  Sonification.log("[TARGET]" +
                    f" distance ${target.node.pos.distance(agent.pos).toInt}%3s," +
                    f" angle ${Math.toDegrees(target.getAngleCorrection(agent)).toInt}%3s°")
                  reducedLogSum = 0
                }
              }
              case None => {
                Sonification.log("Finished!")
                this.stop()
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
        Sonification.log(s"Sound generator initialized in $benchmark ms")
        agent.recorder.start(agent.pos)
        simloop.start()
      }
      case Failure(ex) => Sonification.log("Sound generator init failed")
    }
  }

  def log(line: String) {
    val logger: TextArea = scene.lookup("#log").asInstanceOf[TextArea]
    logger.appendText("\n" + line)
  }
}