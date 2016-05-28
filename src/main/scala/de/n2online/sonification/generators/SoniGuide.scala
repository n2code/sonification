package de.n2online.sonification.generators

import de.n2online.sonification.{Helpers, Route}
import de.sciss.synth.Ops._
import de.sciss.synth._
import de.sciss.synth.ugen._

class SoniGuide() extends Generator {
  private var approval: Option[Synth] = None
  private val approvalMidiNote = 36
  val pentaScale = List(24, 26, 28, 31, 33, 36, 38, 40, 43, 45, 48).map(_+12*2)
  val pentaCenterIndex = 5

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

  private var deltaAcc: Long = 0
  private var lastUpdate: Long = 0
  private var mode = "warning"



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
      val vol = -1.dbamp

      val freq = Line.ar("midiFrom".ir(91.0).midicps, "midiTo".ir(93.0).midicps, 0.05)
      val pure = SinOsc.ar(freq / 2) * 0.1 + SinOsc.ar(freq) * 0.25 + Pulse.ar(freq) * 0.01

      val env = EnvGen.kr(Env.perc , doneAction = 2)

      val sig = pure * env

      Out.ar(List(0, 1), sig * vol)
    })

    approval = Some(Common.approvalSynthDef.get.play(args = List("freq" -> approvalMidiNote.midicps)))
  }

  def assertInitialized() = {
    assert(announcerNote.isDefined)
  }

  override def update(absoluteDistance: Double, correctionAngle: Double, route: Option[Route]): Unit = {
    assertInitialized()
    val now = Helpers.systemTimeInMilliseconds
    deltaAcc = now - lastUpdate

    val angleDegrees = Math.toDegrees(Helpers.wrapToSignedPi(correctionAngle))
    val anglePositive = angleDegrees >= 0

    mode match {
      case "announcer" => {
        if (deltaAcc >= announcerNoteThreshhold) {
          announcerQueue match {
            case next :: after => {
              announcerNote.get.play(args = List(
                "midinote" -> next
              ))
              announcerQueue = after
              deltaAcc = 0
            }
            case Nil =>
          }
        }
      }
      case "warning" => {
        warningAcc += deltaAcc.toInt
        warningWait = Math.abs(angleDegrees) match {
          case angle if angle < 5 => {
            warningNote.get.play(args = List(
              "midiFrom" -> 98,
              "midiTo" -> 98
            ))
            mode = "blubb"
            return
          }
          case angle if angle < 20 => 1500
          case angle if angle < 45 => 1000
          case angle if angle < 90 => 500
          case _ => 250
        }
        warningSequence(warningStep) match {
          case (nextWait, nextStep) if warningAcc >= nextWait() =>
            warningStep match {
              case "hint" => warningNote.get.play(args = List(
                "midiFrom" -> (warningMidi - (if (anglePositive) 2 else -2)),
                "midiTo" -> warningMidi
              ))
              case "ding" => warningNote.get.play(args = List(
                "midiFrom" -> (warningMidi + (if (anglePositive) 5 else -5)),
                "midiTo" -> (warningMidi + (if (anglePositive) 5 else -5))
              ))
              case "wait" =>
            }
            warningStep = nextStep
            warningAcc = 0
          case _ =>
        }

      }
      case _ => {
        if (Math.abs(angleDegrees) > 20) mode = "warning"
      }
    }

    lastUpdate = now
  }

  override def reachedWaypoint(): Unit = {
    approval.get.set("t_trig" -> 1)
    announcerQueue = pentaScale
  }
}
