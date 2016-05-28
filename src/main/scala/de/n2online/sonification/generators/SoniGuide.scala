package de.n2online.sonification.generators

import java.lang.Math.PI

import de.n2online.sonification.{Agent, Helpers, Route, Sonification}
import de.sciss.synth.Ops._
import de.sciss.synth._
import de.sciss.synth.ugen._

class SoniGuide(remainingRelativeMode: Boolean = false, panning: Boolean = true) extends Generator {
  val pentaScale = List(24, 26, 28, 31, 33, 36, 38, 40, 43, 45, 48).map(_+12*2)
  val pentaCenterIndex = 5

  val sync = AnyRef

  private var announcerNote: Option[SynthDef] = None
  private var announcerQueue: List[Int] = List()
  private var announcerAcc = 0
  private var announcerNoteThreshhold = 100


  private var warningNote: Option[SynthDef] = None
  private val warningMidi = 84
  private val warningGlissWidth = 1
  private var warningAcc = 0
  private var warningWait = 2000
  private var warningStep = "hint"
  private val warningSequence = Map(
    ("hint", (() => 0, "ding")),
    ("ding", (() => 300, "wait")),
    ("wait", (() => warningWait, "hint"))
  )

  private val correctionSuccessfulMidi = warningMidi + 12

  private var turner: Option[Synth] = None
  private var turnerOldAngle: Double = 0
  private val turnerMidi = 36
  private var turnerSteady = false
  private var turnerSteadyAcc = 0
  private val turnerSteadyTime = 500
  private val turnCompleteSignal = () => {
    warningNote.get.play(args = List(
      "midiFrom" -> correctionSuccessfulMidi,
      "midiTo" -> correctionSuccessfulMidi
    ))
  }
  private val turnerRemainingCalc = (angle: Double) => {
    Math.min(1.0, Math.max(0.0,
      Math.abs(Helpers.wrapToSignedPi(angle) / (if (remainingRelativeMode) turnerOldAngle else PI))
    ))
  }
  private val turnerPanningCalc = (angle: Double) => {
    if (panning) Math.sin(angle) else 0
  }
  private val turnerVolumeCalc = (angle: Double) => {
    1 - Math.abs(Helpers.wrapToSignedPi(angle)) / PI
  }

  private var deltaAcc: Long = 0
  private var lastUpdate: Long = 0
  private var mode = "walk"

  val setMode = (which: String) => {
    which match {
      case "walk" =>
      case "warning" =>
        warningStep = "hint"
      case "turn" =>
        turnerSteady = false
        turner.get.set(
          "remaining" -> 1.0,
          "midiTo" -> (turnerMidi + 12),
          "holding" -> 1.0,
          "t_trig" -> 1.0
        )
    }
    mode = which
  }


  override def initialize(server: Server): Unit = {
    announcerNote = Some(SynthDef("AnnouncerNote") {
      val vol = "distanceVol".kr(1.0) * -3.dbamp

      val freq = "midinote".ir(36.0).midicps
      val pure = SinOsc.ar(freq / 2) * 0.1 + SinOsc.ar(freq) * 0.25 + Pulse.ar(freq) * 0.05

      import Env.{Segment => Seg}
      val graph = Env(0.001, List(Seg(0.05, 0.dbamp), Seg(0.1, -6.dbamp), Seg(0.5, 0.001)))
      val env = EnvGen.kr(graph, doneAction = 2)

      val sig = pure * env
      Out.ar(List(0, 1), sig * vol)
    })

    warningNote = Some(SynthDef("WarningNote") {
      val vol = -2.dbamp

      val freq = Line.ar("midiFrom".ir(91.0).midicps, "midiTo".ir(93.0).midicps, 0.05)
      val pure = SinOsc.ar(freq / 2) * 0.1 + SinOsc.ar(freq) * 0.25 + Pulse.ar(freq) * 0.01

      val env = EnvGen.kr(Env.perc , doneAction = 2)

      val sig = pure * env

      val panned = LinPan2.ar(sig, LinLin("signedPan".kr(0.0), -1.0, 1.0, -0.7, 0.7))

      Out.ar(0, panned * vol)
    })

    turner = Some(SynthDef("MegaSaw") {
      val numSaws = 7
      val freq = LinLin("remaining".kr(0.0), 1.0, 0.0, turnerMidi.midicps, "midiTo".kr(48.0).midicps)
      val getSaw = (phase: GE) => {
        VarSaw.ar(freq = freq * LinRand(0.99, 1.02), iphase = phase, width = 0.05)
      }
      val accL = getSaw(0.1) + getSaw(0.2) + getSaw(0.3) + getSaw(0.4) + getSaw(0.5) + getSaw(0.6) + getSaw(0.7)
      val accR = getSaw(0.1) + getSaw(0.2) + getSaw(0.3) + getSaw(0.4) + getSaw(0.5) + getSaw(0.6) + getSaw(0.7)

      import Env.{Segment => Seg}
      val hi = Seg(0.1, -3.dbamp)
      val mid = Seg(0.1, -4.dbamp)
      val rise = Seg(0.3, -0.5.dbamp)
      val off = Seg(1, 0)
      val graph = Env(0, List(hi, mid, rise, off), loopNode = 0, releaseNode = 2)
      val gate = "holding".kr(0.0) + "t_trig".tr(0.0)
      val env = EnvGen.kr(graph, gate = gate, doneAction = 0)

      val vol = LinLin("vol".kr(1.0), 0.0, 1.0, -12.dbamp, -1.dbamp)
      val sigL = accL * (1.0 / numSaws) * env * vol
      val sigR = accR * (1.0 / numSaws) * env * vol

      val panned = Balance2.ar(sigL, sigR, LinLin("signedPan".kr(0.0), -1.0, 1.0, -0.9, 0.9))

      Out.ar(0, panned)
    }.play())
  }

  def assertInitialized() = {
    assert(announcerNote.isDefined)
  }

  override def update(absoluteDistance: Double, correctionAngle: Double, route: Option[Route]): Unit = {
    assertInitialized()
    val now = Helpers.systemTimeInMilliseconds
    deltaAcc = now - lastUpdate

    val wrappedAngle = Helpers.wrapToSignedPi(correctionAngle)
    val angleDegrees = Math.toDegrees(wrappedAngle)
    val anglePositive = angleDegrees >= 0

    mode match {
      case "walk" =>
        if (Math.abs(angleDegrees) > 20) {
          setMode("warning")
        } else {

          if (deltaAcc >= announcerNoteThreshhold) {
            announcerQueue match {
              case next :: after =>
                announcerNote.get.play(args = List(
                  "midinote" -> next
                ))
                announcerQueue = after
                deltaAcc = 0
              case Nil =>
            }
          }

        }
      case "turn" =>
        turner.get.set(
          "remaining" -> turnerRemainingCalc(correctionAngle),
          "vol" -> turnerVolumeCalc(correctionAngle),
          "signedPan" -> turnerPanningCalc(correctionAngle)
        )
        angleDegrees match {
          case angle if Math.abs(angle) <= 10 =>
            turnerSteady match {
              case true if turnerSteadyAcc >= turnerSteadyTime =>
                turnCompleteSignal()
                turner.get.set(
                  "remaining" -> 0.0,
                  "vol" -> 1.0,
                  "signedPan" -> 0.0,
                  "holding" -> 0.0
                )
                setMode("walk")
              case true =>
                turnerSteadyAcc += deltaAcc.toInt
              case false =>
                turnerSteadyAcc = 0
                turnerSteady = true
            }
          case _ => turnerSteady = false
        }
      case "warning" =>
        warningAcc += deltaAcc.toInt
        warningWait = Math.abs(angleDegrees) match {
          case angle if angle < 5 =>
            turnCompleteSignal()
            setMode("walk")
            return
          case angle if angle < 20 => 1500
          case angle if angle < 45 => 1000
          case angle if angle < 90 => 500
          case _ => 250
        }
        warningSequence(warningStep) match {
          case (nextWait, nextStep) if warningAcc >= nextWait() =>
            warningStep match {
              case "hint" => warningNote.get.play(args = List(
                "midiFrom" -> (warningMidi - (if (anglePositive) warningGlissWidth else -warningGlissWidth)),
                "midiTo" -> warningMidi,
                "signedPan" -> (if (panning) if (anglePositive) -1.0 else 1.0 else 0.0)
              ))
              case "ding" => warningNote.get.play(args = List(
                "midiFrom" -> (warningMidi + (if (anglePositive) 5 else -5)),
                "midiTo" -> (warningMidi + (if (anglePositive) 5 else -5)),
                "signedPan" -> (if (panning) if (anglePositive) -1.0 else 1.0 else 0.0)
              ))
              case "wait" =>
            }
            warningStep = nextStep
            warningAcc = 0
          case _ =>
        }
      case _ => assert(assertion = false, "Illegal mode: " + mode)
    }

    lastUpdate = now
  }

  override def reachedWaypoint(agent: Agent, route: Route): Unit = {
    route.currentWaypoint match {
      case Some(next) =>
        Helpers.wrapToSignedPi(next.getAngleCorrection(agent)) match {
          case angle if Math.abs(Math.toDegrees(angle)) < 10 =>
            turner.get.set(
              "remaining" -> 0.0,
              "vol" -> 1.0,
              "signedPan" -> 0.0,
              "midiTo" -> (turnerMidi + 12),
              "holding" -> 0.0,
              "t_trig" -> 1.0
            )
            turnCompleteSignal()
            setMode("walk")
          case angle =>
            turnerOldAngle = angle
            setMode("turn")
        }
      case _ =>
    }
  }
}
