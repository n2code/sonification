package de.n2online.sonification

import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}
import javafx.application.{Application, Platform}
import javafx.collections.FXCollections
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.chart.NumberAxis.DefaultFormatter
import javafx.scene.chart.{AreaChart, LineChart, NumberAxis, XYChart}
import javafx.scene.control._
import javafx.scene.input.KeyEvent
import javafx.scene.layout.{AnchorPane, Pane}
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage
import javafx.util.{Callback, Duration}

import de.n2online.sonification.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success, Try}

object SuiteUI {
  private val seedExamples = Array("sonification", "supercollider", "frequency", "amplitude", "phase", "noise", "sine", "sawtooth", "pulse", "envelope", "decay", "reverb", "midi", "opensoundcontrol")
  val maxGraphValues: Int = 200
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
  private var anglePlot: LineChart[Number, Number] = null
  private var distancePlot: AreaChart[Number, Number] = null

  def control[T](id: String) = scene.lookup("#" + id.stripPrefix("#")).asInstanceOf[T]

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
    stage.setMinWidth(1024)
    stage.setMinHeight(700)
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

    //animation

    animation = new Timeline(new KeyFrame(Duration.millis(1000 / Visualization.FPS), new EventHandler[ActionEvent] {
      override def handle(t: ActionEvent): Unit = {
        Sonification.experiment match {
          case Some(exp) => viz.paint(exp)
          case _ => viz.paintStandbyScreen()
        }
      }
    }))
    animation.setCycleCount(Animation.INDEFINITE)

    //sound server

    Sonification.log("[ENGINE] Booting sound server...")
    startSoundServer().onComplete {
      case Success(srv) => {
        Sonification.log("[ENGINE] scsynth booted (see stdout for details), connection established")
        guiDo(() => {
          val setupStep = control[TitledPane]("setupStep")
          setupStep.setDisable(false)
          control[Accordion]("steps").setExpandedPane(setupStep)
        })
      }
      case Failure(err) => Sonification.log("[CRITICAL] Booting server failed: " + err.getMessage)
    }

    //charts

    createCharts()

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
            control[AnchorPane]("analysisWindow").setVisible(false)
          })
        }
        case Failure(err) => Sonification.log("[ERROR] " + err.getMessage)
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

    setButtonHandler("analysisClose", (e) => {
      guiDo(() => {
        control[AnchorPane]("analysisWindow").setVisible(false)
      })
    })

    setButtonHandler("showAnalysis", (e) => {
      guiDo(() => {
        control[AnchorPane]("analysisWindow").setVisible(true)
      })
    })

    control[Pagination]("analysisPage").setPageFactory(new Callback[Integer, javafx.scene.Node] {
      override def call(index: Integer): javafx.scene.Node = {
        Sonification.analysis match {
          case Some(analysis) => {
            anglePlot.getData.set(0, analysis.getAngleData(index))
            distancePlot.getData.set(0, analysis.getDistanceData(index))
          }
          case _ => /* NO-OP */
        }
        new javafx.scene.Group()
      }
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

        Sonification.log(s"[INFO] New test: $routeLength node route on $worldSize with seed " + "\"" + textSeed + "\"")
        val exp = Experiment.build(worldSize, routeLength, rnd, anglePlot) match {
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
                    //moving and updating stats
                    exp.motion.handle(partial, keyboard, exp)

                    //adjusting sound
                    Sonification.sound match {
                      case Some(sound) => {
                        if (sound.getGenerator.isDefined) {
                          sound.getGenerator.get.update(exp.agent.targetDistance, exp.agent.targetAngle)
                        }
                      }
                      case _ => Sonification.log("[ERROR] Sound dead?")
                    }

                    //logging if necessary
                    if (reducedUpdateSum > 100) {
                      val prog = exp.route.visited.length.toDouble / exp.route.waypoints.length
                      val dist = f"${exp.agent.targetDistance.toInt}%3s"
                      val ang = f"${Math.toDegrees(exp.agent.targetAngle).toInt}%3s°"
                      val capinfo = exp.recorder.getPath.length + " records"
                      guiDo(() => updateStatus(prog, dist, ang, capinfo))
                      reducedUpdateSum = 0
                    }
                  }
                  case None => {
                    experimentFinished(exp)
                    stopRunning(exp.simulation)
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
            exp.recorder.start(exp.agent.pos)
            startRunning(exp.simulation)
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

  def experimentFinished(exp: Experiment) = {
    Sonification.log("[INFO] Test finished.")
    stopSound()
    doAnalysis(exp.recorder)
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
      case Some(exp) => {
        stopRunning(exp.simulation)
      }
      case _ => assert(false, "reset triggered but no test running")
    }
  }

  def stopSound() = {
    Sonification.sound match {
      case Some(sman) => sman.freeAll()
      case _ => assert(false, "sound kill triggered but no sound active")
    }
  }

  def createCharts() = {
    guiDo(() => {
      val timeAxis = new NumberAxis()
      timeAxis.setLabel("time in seconds")
      timeAxis.setTickUnit(10)
      timeAxis.setMinorTickCount(9)
      timeAxis.setAutoRanging(true)
      timeAxis.setForceZeroInRange(false)

      val probablyMaxDistance = MeshBuilder.defaultCellSize * (1 + 2 * MeshBuilder.centerVariance)
      val distanceAxis = new NumberAxis(Waypoint.thresholdReached, probablyMaxDistance, probablyMaxDistance - Waypoint.thresholdReached)
      distanceAxis.setMinorTickCount(0)
      distanceAxis.setTickLabelFormatter(new DefaultFormatter(distanceAxis) {
        override def toString(n: Number) = ""
      })
      distancePlot = new AreaChart[Number, Number](timeAxis, distanceAxis, FXCollections.observableArrayList(new XYChart.Series[Number, Number]()))
      distancePlot.setCreateSymbols(false)
      distancePlot.setHorizontalGridLinesVisible(false)
      distancePlot.setVerticalGridLinesVisible(false)
      distancePlot.getStylesheets.addAll(getClass.getResource("/distanceChart.css").toExternalForm)
      distancePlot.setPadding(new Insets(5, 5, 5, 13))

      val angleAxis = new NumberAxis(-180, 180, 30)
      angleAxis.setMinorTickCount(5)
      angleAxis.setTickLabelFormatter(new DefaultFormatter(angleAxis) {
        override def toString(n: Number) = f"${n.intValue}%4s°"
      })
      anglePlot = new LineChart[Number, Number](timeAxis, angleAxis, FXCollections.observableArrayList(new XYChart.Series[Number, Number]()))
      anglePlot.setCreateSymbols(false)
      anglePlot.setAlternativeRowFillVisible(false)
      anglePlot.setAlternativeColumnFillVisible(false)
      anglePlot.getStylesheets.addAll(getClass.getResource("/angleChart.css").toExternalForm)

      val setCommonProps = (chart: XYChart[Number, Number]) => {
        chart.setLegendVisible(false)
        chart.setAnimated(false)
        AnchorPane.setTopAnchor(chart, 0.0)
        AnchorPane.setLeftAnchor(chart, 0.0)
        AnchorPane.setRightAnchor(chart, 0.0)
        AnchorPane.setBottomAnchor(chart, 0.0)
      }
      setCommonProps(distancePlot)
      setCommonProps(anglePlot)

      val plotPane = control[AnchorPane]("plotPane")
      plotPane.getChildren.add(distancePlot)
      plotPane.getChildren.add(anglePlot)
    })
  }

  def doAnalysis(recorder: PathRecorder) = {
    val analysis = Analysis.execute(recorder)
    val analysisPage = control[Pagination]("analysisPage")
    analysisPage.setMaxPageIndicatorCount(1337)
    analysisPage.setPageCount(analysis.getDataSetCount)
    analysisPage.setCurrentPageIndex(0)
  }

  def startRunning(sim: AnimationTimer) = {
    animation.play()
    keyboard.consumeEvents = true
    sim.start()
  }

  def stopRunning(sim: AnimationTimer) = {
    sim.stop()
    keyboard.consumeEvents = false
    animation.pause()
  }
}
