package de.n2online.sonification

import java.io.FileWriter

object Sonification {
  private val logFilePath = "Sonification_" + Helpers.sortableDate + ".log"
  var gui: SuiteUI = null
  var sound: Option[SoundManager] = None
  var experiment: Option[Experiment] = None
  var analysis: Option[Analysis] = None

  def main(args: Array[String]) {
    javafx.application.Application.launch(classOf[SuiteUI], args: _*)
  }

  def log(text: String): Unit = {
    //log to file
    val logger = new FileWriter(logFilePath, true)
    try logger.write(text + "\n") finally logger.close()
    //log to console or GUI
    if (gui != null) gui.log(text) else println(s"=> $text")
  }
}

