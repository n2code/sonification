package de.n2online.sonification

import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}
import javafx.application.Application
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXMLLoader
import javafx.scene.{Parent, Scene}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control._
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Pane
import javafx.stage.Stage
import javafx.util.Duration

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success, Try}


class SuiteUI extends Application {
  private var root: Parent = null
  private var scene: Scene = null
  private var screen: Canvas = null
  private var gc: GraphicsContext = null
  private var keyboard: Keyboard = null
  private var viz: Visualization = null
  private var monitor: Pane = null
  private var animation: Timeline = null
  private var experiment: Option[Experiment] = None

  def control[T](id: String) = scene.lookup("#"+id.stripPrefix("#")).asInstanceOf[T]

  override def stop(): Unit = {
    //TODO: anti-get
    Sonification.sound.get.stopServer()
  }

  @throws[Exception]
  def start(stage: Stage) {
    //GUI init

    root = FXMLLoader.load(getClass.getResource("/Main.fxml"))
    stage.setTitle("Sonification-Suite")
    scene = new Scene(root)
    stage.setScene(scene)
    stage.show
    Sonification.gui = this

    monitor = control[Pane]("monitor")
    screen = scene.lookup("#screen").asInstanceOf[Canvas]
    screen.widthProperty.bind(monitor.widthProperty)
    screen.heightProperty.bind(monitor.heightProperty)
    gc = screen.getGraphicsContext2D

    //handling movement and input

    keyboard = new Keyboard
    scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler[KeyEvent] {
      override def handle(evt: KeyEvent): Unit = keyboard.registerKeyDown(evt)
    })
    scene.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler[KeyEvent] {
      override def handle(evt: KeyEvent): Unit = keyboard.registerKeyUp(evt)
    })

    //animation is always running :)

    animation = new Timeline(new KeyFrame(Duration.millis(1000 / 25), new EventHandler[ActionEvent] {
      override def handle(t: ActionEvent): Unit = {
        experiment match {
          case Some(exp) => viz.paint(exp.agent, exp.route, exp.mesh)
          case _ => {/*NOOP*/}
        }
      }
    }))
    animation.setCycleCount(Animation.INDEFINITE)
    animation.play()

    //INSTA-START

    startSoundServer()
    startExperiment() match {
      case Success(exp) => experiment = Some(exp)
      case Failure(err) => Sonification.log(err.getMessage)
    }
  }

  def log(line: String) {
    control[TextArea]("log").appendText("\n" + line)
  }

  def startSoundServer() = {
    Sonification.sound = Some(new SoundManager)
  }

  def startExperiment(): Try[Experiment] = {
    Sonification.sound match {
      case Some(sman) => {
        val generatorReady = sman.setGenerator(new generators.PanningScale)

        //data setup

        val worldSize = Rectangle(800, 400)
        val exp = new Experiment(worldSize, 10)

        //graphics
        viz = new Visualization(gc, monitor.getWidth, monitor.getHeight)
        viz.viewport = Rectangle(worldSize.width, worldSize.height, 0, 0)

        //calculation (sim & sound) :
        val simloop = new AnimationTimer() {

          private var last: Long = 0
          private var deltaSum: Long = 0
          private val recalcThreshold: Long = 10
          private var reducedUpdateSum: Long = 0

          def handle(tstamp: Long) {
            val now = tstamp / 1000000
            if (last != 0) {
              val delta = now - last
              deltaSum += delta
              reducedUpdateSum += delta

              if (deltaSum >= recalcThreshold) {
                val partial: Double = deltaSum / 1000.0

                exp.route.currentWaypoint match {
                  case Some(target) => {
                    exp.mot.handle(partial, keyboard, exp.agent, exp.route)
                    Sonification.sound match {
                      case Some(sman) => {
                        if (sman.getGenerator.isDefined) {
                          val dist = target.node.pos.distance(exp.agent.pos)
                          val angle = target.getAngleCorrection(exp.agent)
                          sman.getGenerator.get.update(dist, angle)
                        }
                      }
                      case _ => Sonification.log("Sound dead?")
                    }
                    if (reducedUpdateSum > 100) {
                      val dist = f"${target.node.pos.distance(exp.agent.pos).toInt}%3s"
                      val ang = f"${Math.toDegrees(target.getAngleCorrection(exp.agent)).toInt}%3s°"
                      scene.lookup("#currentTargetDistance").asInstanceOf[TextField].setText(dist)
                      scene.lookup("#currentTargetAngle").asInstanceOf[TextField].setText(ang)
                      reducedUpdateSum = 0
                    }
                  }
                  case None => {
                    Sonification.log("Finished!")
                    this.stop()
                    keyboard.consumeEvents = false
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
            exp.agent.recorder.start(exp.agent.pos)
            keyboard.consumeEvents = true
            simloop.start()
          }
          case Failure(ex) => Sonification.log("Sound generator init failed")
        }

        Success(exp)
      }
      case _ => Failure(new Throwable("Sound server not initialized"))
    }
  }
}
