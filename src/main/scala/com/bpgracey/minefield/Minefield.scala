/*
 * Copyright 2018 Bancroft P. Gracey
 * 
 *  This file is part of Minefield.
 *
 *  Minefield is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  Minefield is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Minefield.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.bpgracey.minefield

import scala.annotation.tailrec
import scala.io.StdIn
import scala.math.{abs, floor, pow, sqrt}
import scala.util.{Failure, Random, Success, Try}

/**
 * Create a field and populate it with mines
 */
object Field {
  def apply(): Field = apply(40, 40, 8)
  
  def apply(maxX: Int, maxY: Int, maxBombs: Int) = new Field(maxX, maxY, makeBombs(maxX, maxY, maxBombs))
    
  protected[minefield] def makeBombs(maxX: Int, maxY: Int, maxBombs: Int = 8): Set[Point] = makeBombs(Set.empty, maxBombs, maxX, maxY)
  
  @tailrec
  protected def makeBombs(bombs: Set[Point], maxBombs: Int, maxX: Int, maxY: Int): Set[Point] = {
    val newBombs = bombs + Point(Random.nextInt(maxX), Random.nextInt(maxY))
    if (newBombs.size >= maxBombs) newBombs else makeBombs(newBombs, maxBombs, maxX, maxY)
  }
}

/**
 * A field requiring sweeping
 */
case class Field (maxX: Int, maxY: Int, bombs: Set[Point]) {
  require (maxX > 0 && maxY > 0 & !bombs.isEmpty)
  val numBombs = bombs.size
  
  def hasInBounds(p: Point) = 0 <= p.x && p.x < maxX && 0 <= p.y && p.y < maxY
  
  def hasBombAt(p: Point) = bombs.contains(p)
  
  protected[minefield] lazy val zeros = (0 until maxX) flatMap { x =>
    (0 until maxY) map { y => Point(x, y) }
  } filterNot (bombs.contains _) filter (p => bombs.forall(q => (p - q) >= 2))
  
  val size = maxX * maxY - numBombs
}

/**
 * A point in a field (or possibly outside it)
 */
case class Point(x: Int, y: Int) {
  //require(x >= 0 && y >= 0)
  import Point._
  
  lazy val locals = sides map (this + _)
  
  /**
   * neighbouring Points in the current Field
   */
  def neighbours(implicit field: Field) = locals filter(field.hasInBounds(_))
  
  /**
   * add a tuple2 to the current point to get a new point
   */
  def + (tuple: (Int, Int)) = tuple match {case (a, b) => Point(a + x, b + y)}
  
  /**
   * subtract another point to get the distance in Points between them
   */
  def - (point: Point) = floor(sqrt(pow(point.x - x, 2) + pow(point.y - y, 2))).toInt
}

object Point {
  private val sideRange = -1 to 1
  protected val sides = sideRange flatMap (a => sideRange map (b => (a, b))) filter (_ != (0, 0))
}

/**
 * Wrapper for a Point in a Field
 */
sealed abstract class DisplayPoint(p: Point)(implicit field: Field) {
  val point = p
  def isBomb: Boolean
  def bombCount: Int
}

/**
 * Generate the DisplayPoint for a Point in a Field
 */
object DisplayPoint {
  def apply(p: Point)(implicit field: Field) = if (field.hasBombAt(p)) BombPoint(p) else ClearPoint(p)
  
  def apply(x: Int, y: Int)(implicit field: Field): DisplayPoint = apply(Point(x, y))
  
  def unapply(dp: DisplayPoint): Option[Point] = Some(dp.point)
}

/**
 * A Point in a Field that covers a Bomb
 */
case class BombPoint(p: Point)(implicit field: Field) extends DisplayPoint(p) {
  override val isBomb = true
  override val bombCount = -1
}
/**
 * A Point in a Field that does not cover a bomb
 */
case class ClearPoint(p: Point)(implicit field: Field) extends DisplayPoint(p) {
  override val isBomb = false
  override lazy val bombCount = p.neighbours(field).filter(field.hasBombAt(_)).size
  lazy val zeroCount = bombCount == 0
  lazy val nonZeroCount = !zeroCount
}

/**
 * Utility object for splitting a list into two lists
 */
object Sep {
  implicit class SepList[T](list: List[T]) {
    /**
     * Separate a list into 2 lists, preserving order
     * @param p function to determine inclusion in first list
     * @return Tuple2 where _1 is list where p is true, _2 is remainder of elements
     */
    def separate(p: T => Boolean): (List[T], List[T]) = 
      list.foldLeft((List[T](), List[T]()))((tup, t) => if (p(t)) (tup._1 :+ t, tup._2) else (tup._1, tup._2 :+ t))
  }
}

/**
 * The state of a game - lost, won, or still playing
 */
sealed trait GameState
case object Playing extends GameState
case object Lost extends GameState
case object Won extends GameState

/**
 * The state of the current playing field
 */
case class FieldState(display: Map[Point, DisplayPoint])(implicit field: Field) {
  val state: GameState = if (display.values.exists(_.isBomb)) Lost else checkWon(display)
  
  protected def checkWon(newDisplay: Map[Point, DisplayPoint]) = if (newDisplay.size >= field.size) Won else Playing
  
  /**
   * Utility for updating the Map of DisplayPoints
   */
  implicit class DisplayMap(map: Map[Point, DisplayPoint]) {
    /**
     * Add a display point
     */
    def :+ (dp: DisplayPoint): Map[Point, DisplayPoint] = map + (dp.point -> dp)
    /**
     * Add a list of DisplayPoints (eg. after a zero point is touched)
     */
    def :++ (ldp: List[DisplayPoint]): Map[Point, DisplayPoint] = map ++ (ldp.map(p => (p.point -> p)))
  }
  
  /**
   * The Points in the Field which are zeros (not touching bombs) and which have not been displayed yet
   */
  protected lazy val zeros = field.zeros filterNot (display contains _)
  
  /**
   * A random zero point
   * @return None if no zeros remain, or Some[Point] a randomly-selected zero
   */
  def zero(): Option[Point] = if (zeros.isEmpty) None else Some(zeros(Random.nextInt(zeros.length)))
  
  /**
   * Touch a Point if it exists. For use with zero()
   * @param op Option of Point to be touched
   * @return new FieldState
   */
  def touch(op: Option[Point]): FieldState = op map (this.touch _) getOrElse this
  
  /**
   * Touch a Point
   * @param p Point to be touched
   * @return new FieldState
   */
  def touch(p: Point): FieldState = touch(p.x, p.y)
  
  /**
   * Touch a Point defined by x & y
   * @param x column
   * @param y row
   * @return new FieldState
   */
  def touch(x: Int, y: Int): FieldState = {
    require(state == Playing)
    
    DisplayPoint(x, y) match {
      case bp @ BombPoint(_) => // it's a bomb - you lose!
        FieldState(display :+ bp :++ (field.bombs.map(BombPoint(_)).toList))
      case cp @ ClearPoint(_) if cp.zeroCount => // it's a zero - find its neighbours!
        val newDisp = expandDisplay(display, List(cp))
        FieldState(newDisp)
      case cp @ ClearPoint(_) => // it's next to a bomb - display it!
        val newDisp = display :+ cp
        FieldState(newDisp)
    }
  }
  
  /*
   * generate the updates when a zero is touched
   */
  @tailrec
  protected final def expandDisplay(disp: Map[Point, DisplayPoint], points: List[ClearPoint])(implicit field: Field): Map[Point, DisplayPoint] =
    points match {
      case Nil => disp
      case p :: np if disp.contains(p.p) =>
        expandDisplay(disp, np)
      case p :: np if p.nonZeroCount =>
        expandDisplay(disp :+ p, np)
      case p :: np =>
        import Sep._
        val (zeros, nonzeros) = p.p.neighbours.filterNot(disp.contains(_)).map(ClearPoint(_)).toList.separate(_.zeroCount)
        expandDisplay(disp :+ p :++ nonzeros, np ++ zeros)
    }
  
  /**
   * For a completed Game, returns the FieldState with all bombs marked
   */
  def completed: FieldState = {
    require(state != Playing)
    FieldState(display :++ (field.bombs.map(BombPoint(_)).toList))
  }
  
  /**
   * List of points to display with values (0 - 8, number of bombs touched). Bombs have value 9. For use with display APIs
   */
  lazy val displayData = (display.values map (dp => ((dp.point.x, dp.point.y) -> dp.bombCount))).toMap
  
  /**
   * Game state - 0 = playing, -1 = lost, 1 = won. For use with display APIs
   */
  lazy val displayState = if (state == Playing) 0 else if (state == Won) 1 else -1
}

/**
 * create an initial FieldState
 */
object FieldState {
  def apply()(implicit field: Field) = new FieldState(Map.empty)
}