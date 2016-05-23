package de.n2online.sonification

import javafx.scene.chart.XYChart

import scala.collection.mutable.MutableList

object Analysis {
  def execute(recorder: PathRecorder) = {
    /*
    val seriesData = anglePlot.getData.get(0).getData
    seriesData.add(new XYChart.Data[Number, Number](tTotal / 1000.0, Math.toDegrees(agent.targetAngle)))
    if (seriesData.size > SuiteUI.maxGraphValues) seriesData.remove(0, seriesData.size - SuiteUI.maxGraphValues - 1)
    */

    Sonification.log("[INFO] Analysing data...")
    var analysis = new Analysis
    val path = recorder.getPath
    path.foreach(record => {
      analysis.addDataEntry(record.agentDistance, record.agentAngle, record.millisecondTotal)
      if (record.reachedWaypoint.isDefined && record != path.last) analysis.createNextDataSet()
    })
    Sonification.analysis = Some(analysis)
    Sonification.log(s"[INFO] Extracted ${analysis.getDataSetCount} waypoint plots with a total of ${analysis.getRecordCount} records.")
    analysis
  }
}

class Analysis {
  private val distanceData = new MutableList[XYChart.Series[Number, Number]]
  private val angleData = new MutableList[XYChart.Series[Number, Number]]
  private var recordCount: Int = 0
  createNextDataSet()

  def createNextDataSet() = Array(distanceData, angleData).foreach(_ += new XYChart.Series[Number, Number])

  def addDataEntry(distance: Double, angle: Double, totalMilliseconds: Long) = {
    val seconds = totalMilliseconds / 1000.0
    distanceData.last.getData.add(new XYChart.Data(seconds, distance))
    angleData.last.getData.add(new XYChart.Data(seconds, Math.toDegrees(angle)))
    recordCount += 1
  }

  def getRecordCount = recordCount

  def getDataSetCount = distanceData.length

  def getDistanceData(index: Int) = distanceData(index)

  def getAngleData(index: Int) = angleData(index)
}
