package de.n2online.sonification

import java.nio.file.{Files, Paths}
import javafx.scene.chart.XYChart

import scala.collection.mutable.MutableList
import scala.pickling._
import scala.pickling.binary._
import scala.util.{Failure, Success, Try}

object Analysis {
  def execute(recorder: PathRecorder) = {
    Sonification.log("[INFO] Analysing data...")
    val path = recorder.getPath
    val analysis = new Analysis

    analysis.createNextDataSet()
    path.foreach(record => {
      analysis.addDataEntry(record.agentDistance, record.agentAngle, record.millisecondTotal)
      if (record.reachedWaypoint.isDefined && record != path.last) analysis.createNextDataSet()
    })

    Sonification.analysis = Some(analysis)
    Sonification.log(s"[INFO] Extracted ${analysis.getDataSetCount} waypoint plots with a total of ${analysis.getRecordCount} records.")
    analysis
  }

  def saveToFile(obj: Analysis, path: String): Try[Boolean] = {
    try {
      val fileData: BinaryPickle = obj.exportData.pickle
      Files.write(Paths.get(path), fileData.value)
      Success(true)
    } catch {
      case err: Throwable => Failure(err)
    }
  }

  def loadFromFile(path: String): Try[Analysis] = {
    try {
      val restored = new Analysis
      val archived = Files.readAllBytes(Paths.get(path)).unpickle[AnalysisData]
      restored.importData(archived)
      Sonification.analysis = Some(restored)
      Sonification.log(s"[INFO] Loaded archived analysis with ${restored.getDataSetCount} waypoint plots.")
      Success(restored)
    } catch {
      case _: Throwable => Failure(new RuntimeException("Loading saved analysis failed"))
    }
  }
}

class Analysis {
  private val distanceData = new MutableList[XYChart.Series[Number, Number]]
  private val angleData = new MutableList[XYChart.Series[Number, Number]]
  private val archiveData = new MutableList[MutableList[AnalysisRecord]]
  private var recordCount: Int = 0

  def createNextDataSet() = {
    Array(distanceData, angleData).foreach(_ += new XYChart.Series[Number, Number])
    archiveData += new MutableList[AnalysisRecord]
  }

  def addDataEntry(distance: Double, angle: Double, totalMilliseconds: Long) = {
    val seconds = totalMilliseconds / 1000.0
    distanceData.last.getData.add(new XYChart.Data(seconds, distance))
    angleData.last.getData.add(new XYChart.Data(seconds, Math.toDegrees(angle)))
    archiveData.last += AnalysisRecord(distance, angle, totalMilliseconds)
    recordCount += 1
  }

  def getRecordCount = recordCount

  def getDataSetCount = distanceData.length

  def getDistanceData(index: Int) = distanceData(index)

  def getAngleData(index: Int) = angleData(index)

  def exportData = AnalysisData(archiveData.map(_.toList).toList)

  def importData(archived: AnalysisData) = {
    archived.data.foreach(set => {
      this.createNextDataSet()
      set.foreach(record => this.addDataEntry(record.d, record.a, record.t))
    })
  }
}

case class AnalysisRecord(d: Double, a: Double, t: Long)
case class AnalysisData(data: List[List[AnalysisRecord]])
