package de.n2online.sonification

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

import scala.util.{Failure, Random, Success, Try}

object MeshBuilder {
  //settings
  val defaultCellSize = 100 //used if no number of cells per dimension is given
  val centerVariance = 0.3 //center varied by 30% of the cells width and height
  val pCellEmpty = 0.15 //chance that a cell contains no node


  def getRandomMesh(
                     topleft: Vector2D,
                     width: Double, height: Double,
                     randomSource: Random,
                     cellNumX: Option[Int] = None, cellNumY: Option[Int] = None
                   ): Try[Graph] = {
    //reproducable randomness
    def randomVariance: Double = {
      //random signed variance < abs(centerVariance)
      randomSource.nextDouble() * 2 * centerVariance - centerVariance
    }

    //calculate dimensions
    val cellsX: Int = cellNumX.getOrElse((width / defaultCellSize).floor.toInt)
    val cellsY: Int = cellNumY.getOrElse((height / defaultCellSize).floor.toInt)

    val cellWidth = width / cellsX
    val cellHeight = height / cellsY

    //generate nodes
    val nodes: Set[Node] = List.tabulate[Option[Node]](cellsX, cellsY) { (x, y) => {
      //calculate cell center with random variance of both dimensions by centerVariance
      val nodeX = topleft.getX + (x + 0.5 + randomVariance) * cellWidth
      val nodeY = topleft.getY + (y + 0.5 + randomVariance) * cellHeight

      if (randomSource.nextDouble() < pCellEmpty) None else Some(Node(nodeX, nodeY, Cell(x, y, cellWidth, cellHeight)))
    }
    }.flatten.flatten.toSet //we now got all nodes

    if (nodes.size < 4) return Failure(new IllegalArgumentException("Less than four nodes generated. Bad luck / check parameters!"))

    val edges: Set[Edge] = nodes.flatMap(from => {
      //connect nodes of adjacent cells
      val targets = nodes.filter { to =>
        (
          (from.cell.x - 1 to from.cell.x + 1).contains(to.cell.x) //neighbourhood constraint X
            && (from.cell.y - 1 to from.cell.y + 1).contains(to.cell.y) //neighbourhood constraint Y
            && (from.cell.x, from.cell.y) !=(to.cell.x, to.cell.y) //...and no reflexive edge relation please :)
          )
      }
      targets.map(Edge(from, _))
    })

    Success(new Graph(nodes, edges))
  }

}
