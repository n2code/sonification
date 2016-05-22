package de.n2online.sonification

import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}
import javafx.application.{Application, Platform}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXMLLoader
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control._
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Pane
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage
import javafx.util.Duration

import de.n2online.sonification.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success, Try}

object SuiteUI {
  private val seedExamples = Array("sonification", "supercollider", "frequency", "amplitude", "phase", "noise", "sine", "sawtooth", "pulse", "envelope", "decay", "reverb", "midi", "opensoundcontrol")
}

class SuiteUI extends Application {
  private var root: Parent = null
  private var scene: Scene = null
  private var screen: Canvas = null
  private var gc: GraphicsContext = null
  private var keyboard: Keyboard = null
  private var viz: Visualization = null
  private var monitor: Pane = null
  private var animation: Timeline = null

  def control[T](id: String) = scene.lookup("#"+id.stripPrefix("#")).asInstanceOf[T]

  override def stop(): Unit = {
    //TODO: anti-get
    Sonification.sound match {
      case Some(sman) => sman.stopServer()
      case _ => /* not initialized, no need to shut down */
    }
  }

  @throws[Exception]
  def start(stage: Stage) {
    //stage init

    root = FXMLLoader.load(getClass.getResource("/Main.fxml"))
    stage.setTitle("Sonification-Suite")
    scene = new Scene(root)
    stage.setScene(scene)
    stage.show
    Sonification.gui = this

    //main graphics area

    monitor = control[Pane]("monitor")
    screen = scene.lookup("#screen").asInstanceOf[Canvas]
    screen.widthProperty.bind(monitor.widthProperty)
    screen.heightProperty.bind(monitor.heightProperty)
    gc = screen.getGraphicsContext2D
    viz = new Visualization(gc, screen.getWidth, screen.getHeight)

    //handling movement and input

    keyboard = new Keyboard
    scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler[KeyEvent] {
      override def handle(evt: KeyEvent): Unit = keyboard.registerKeyDown(evt)
    })
    scene.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler[KeyEvent] {
      override def handle(evt: KeyEvent): Unit = keyboard.registerKeyUp(evt)
    })

    //animation is always running :)

    animation = new Timeline(new KeyFrame(Duration.millis(1000 / Visualization.FPS), new EventHandler[ActionEvent] {
      override def handle(t: ActionEvent): Unit = {
        Sonification.experiment match {
          case Some(exp) => viz.paint(exp.agent, exp.route, exp.mesh)
          case _ => viz.paintStandbyScreen()
        }
      }
    }))
    animation.setCycleCount(Animation.INDEFINITE)
    animation.play()

    //sound server

    Sonification.log("[ENGINE] Booting SuperCollider server...")
    startSoundServer().onComplete {
      case Success(srv) => {
        Sonification.log("[ENGINE] scsynth booted and connection established.")
        guiDo(() => {
          val setupStep = control[TitledPane]("setupStep")
          setupStep.setDisable(false)
          control[Accordion]("steps").setExpandedPane(setupStep)
        })
      }
      case Failure(err) => Sonification.log("[CRITICAL] Booting server failed: "+err.getMessage)
    }

    //bind controls, set default values

    setButtonHandler("startTest", (e) => {
      startExperiment() match {
        case Success(exp) => {
          Sonification.experiment = Some(exp)

          guiDo(() => {
            blockSetupParameters(true)
            val statusStep = control[TitledPane]("statusStep")
            statusStep.setDisable(false)
            control[TitledPane]("resultsStep").setDisable(true)
            control[Accordion]("steps").setExpandedPane(statusStep)
          })
        }
        case Failure(err) => Sonification.log("[ERROR] "+err.getMessage)
      }
    })

    setButtonHandler("restartTest", (e) => {
      control[Button]("startTest").fire()
    })

    setButtonHandler("randomSeed", (e) => {
      guiDo(() => {
        val randomSeed = SuiteUI.seedExamples(Random.nextInt(SuiteUI.seedExamples.length))
        control[TextField]("seed").setText(randomSeed)
      })
    })

    setButtonHandler("resetTest", (e) => {
      resetExperiment()
      guiDo(() => {
        control[TitledPane]("statusStep").setDisable(true)
        blockSetupParameters(false)
      })
    })

    control[TextField]("worldWidth").setText("800")
    control[TextField]("worldHeight").setText("400")
    control[Slider]("routeLength").setValue(10)
  }

  def setButtonHandler(buttonId: String, handler: (ActionEvent) => Unit) = {
    control[Button](buttonId).setOnAction(new EventHandler[ActionEvent] {
      override def handle(t: ActionEvent): Unit = handler(t)
    })
  }

  def log(line: String) {
    guiDo(() => control[TextArea]("log").appendText("\n" + line))
  }

  def startSoundServer() = {
    val sman = new SoundManager
    Sonification.sound = Some(sman)
    sman.server
  }

  def startExperiment(): Try[Experiment] = {
    Sonification.sound match {
      case Some(sman) => {

        //data setup

        val worldSize = (
          tryToInt(control[TextField]("worldWidth").getText),
          tryToInt(control[TextField]("worldHeight").getText)
          ) match {
          case (Some(wW), Some(wH)) => Rectangle(wW, wH)
          case _ => return Failure(new Throwable("World width or height has invalid format"))
        }

        val textSeed = control[TextField]("seed").getText
        val rnd = new Random(textSeed.hashCode)

        val routeLength = control[Slider]("routeLength").getValue.toInt

        Sonification.log(s"[INFO] New test: $routeLength node route on $worldSize with seed "+"\""+textSeed+"\"")
        val exp = Experiment.build(worldSize, routeLength, rnd) match {
          case Success(scenario) => scenario
          case Failure(err) => return Failure(err)
        }

        //sound

        val generatorReady = sman.setGenerator(new generators.PanningScale)

        //graphics

        viz = new Visualization(gc, monitor.getWidth, monitor.getHeight)
        viz.viewport = Rectangle(worldSize.width, worldSize.height, 0, 0)

        //calculation (simulation & sound updates) :

        exp.simulation = new AnimationTimer() {

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
                    exp.motion.handle(partial, keyboard, exp.agent, exp.route)
                    Sonification.sound match {
                      case Some(sound) => {
                        if (sound.getGenerator.isDefined) {
                          val dist = target.node.pos.distance(exp.agent.pos)
                          val angle = target.getAngleCorrection(exp.agent)
                          sound.getGenerator.get.update(dist, angle)
                        }
                      }
                      case _ => Sonification.log("[ERROR] Sound dead?")
                    }
                    if (reducedUpdateSum > 100) {
                      val prog = exp.route.visited.length.toDouble / exp.route.waypoints.length
                      val dist = f"${target.node.pos.distance(exp.agent.pos).toInt}%3s"
                      val ang = f"${Math.toDegrees(target.getAngleCorrection(exp.agent)).toInt}%3s°"
                      val capinfo = exp.agent.recorder.getPath.length+" records"
                      guiDo(() => updateStatus(prog, dist, ang, capinfo))
                      reducedUpdateSum = 0
                    }
                  }
                  case None => {
                    experimentFinished()
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

        //ready for takeoff

        generatorReady.onComplete {
          case Success(benchmark) => {
            Sonification.log(s"[ENGINE] Sound generator initialized in $benchmark ms")
            exp.agent.recorder.start(exp.agent.pos)
            keyboard.consumeEvents = true
            exp.simulation.start()
          }
          case Failure(ex) => Sonification.log("[ERROR] Sound generator init failed")
        }

        Success(exp) //now only sound could fail
      }
      case _ => Failure(new Throwable("Sound server not initialized"))
    }
  }

  def updateStatus(progress: Double, distance: String, angle: String, captureInfo: String) = {
    control[ProgressBar]("routeProgress").setProgress(progress)
    control[TextField]("currentTargetDistance").setText(distance)
    control[TextField]("currentTargetAngle").setText(angle)
    control[Label]("captureInfo").setText(captureInfo)
  }

  def blockSetupParameters(blocked: Boolean) = {
    control[TextField]("seed").setDisable(blocked)
    control[Button]("randomSeed").setDisable(blocked)
    control[TextField]("worldWidth").setDisable(blocked)
    control[TextField]("worldHeight").setDisable(blocked)
    control[Slider]("routeLength").setDisable(blocked)
    control[Button]("startTest").setDisable(blocked)
    control[Button]("resetTest").setDisable(!blocked)
  }

  def guiDo(func: () => Unit) = {
    Platform.runLater(new Runnable {
      override def run() = func()
    })
  }

  def experimentFinished() = {
    Sonification.log("[INFO] Test finished.")
    stopSound()
    guiDo(() => {
      control[TitledPane]("statusStep").setDisable(true)
      val resultsStep = control[TitledPane]("resultsStep")
      resultsStep.setDisable(false)
      control[Accordion]("steps").setExpandedPane(resultsStep)
      blockSetupParameters(false)
    })
  }

  def resetExperiment() = {
    Sonification.log("[INFO] Test interrupted.")
    stopSound()
    Sonification.experiment match {
      case Some(exp) => exp.simulation.stop()
      case _ => assert(false, "reset triggered but no test running")
    }
  }

  def stopSound() = {
    Sonification.sound match {
      case Some(sman) => sman.freeAll()
      case _ => assert(false, "sound kill triggered but no sound active")
    }
  }
}
