package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

import scala.util.Random

object MeshBuilder {

  def getRandomMesh(
                    topleft: Vector2D,
                    width: Double, height: Double,
                    cellNumX: Option[Int] = None, cellNumY: Option[Int] = None,
                    seed: Int = Random.nextInt()
                   ): List[Node] = {
    //settings
    val defaultCellSize = 100 //used if no number of cells per dimension is given
    val centerVariance = 0.3 //center varied by 30% of the cells width and height
    val pCellEmpty = 0.05 //chance that a cell contains no node

    //reproducable randomness
    val rnd = new Random(seed)
    def randomVariance: Double = {
      //random signed variance < abs(centerVariance)
      rnd.nextDouble()*2*centerVariance - centerVariance
    }

    //calculate dimensions
    val cellsX: Int = cellNumX.getOrElse((width/defaultCellSize).floor.toInt)
    val cellsY: Int = cellNumY.getOrElse((height/defaultCellSize).floor.toInt)

    val cellWidth = width / cellsX
    val cellHeight = width / cellsY

    //populate with Nodes
    val cells = List.tabulate[Cell](cellsX, cellsY) { (x, y) => {
      //calculate cell center with random variance of both dimensions by centerVariance
      val nodeX = topleft.getX + (x + 0.5 + randomVariance)*cellWidth
      val nodeY = topleft.getY + (y + 0.5 + randomVariance)*cellHeight

      Cell(
        x, y,
        cellWidth, cellHeight,
        if (rnd.nextDouble()<pCellEmpty) None else Some(new Node(nodeX, nodeY))
      )
    }}.flatten //we now got all cells - empty or not

    //connect [existing] nodes of adjacent cells
    cells.filter(_.node.isDefined).foreach(cell => {
      //find valid targets and calculate edges to them
      val targets = for (validTarget <- cells.filter(target =>
        (cell.x-1 to cell.x+1).contains(target.x) //neighbourhood constraint X
        && (cell.y-1 to cell.y+1).contains(target.y) //neighbourhood constraint Y
        && target.node.isDefined //node exists
        && (cell.x, cell.y) != (target.x, target.y) //...and no reflexive edge relation please :)
      )) yield Edge(cell.node.get, validTarget.node.get)

      //and finally do what we came for: update the current cells edges
      cell.node.get.edges = targets.toSet
    })

    cells.flatMap(_.node)
  }

}
