package de.n2online.sonification

object Sonification {
  var gui: SuiteUI = null
  var sound: Option[SoundManager] = None
  var experiment: Option[Experiment] = None
  var analysis: Option[Analysis] = None

  def main(args: Array[String]) {
    javafx.application.Application.launch(classOf[SuiteUI], args: _*)
  }

  def log(text: String): Unit = {
    if (gui != null) gui.log(text)
    else println(s"=> $text")
  }
}

