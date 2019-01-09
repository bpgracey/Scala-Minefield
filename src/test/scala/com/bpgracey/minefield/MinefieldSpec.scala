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

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import scala.util.Random

class MinefieldSpec extends FlatSpec with Matchers {
  "Field" should "correctly figure in & out of bounds" in {
    val maxX = 10
    val maxY = 5
    val field = Field(maxX, maxY, 1)
    val tests = Seq((Point(1, 1), true), (Point(20, 20), false), (Point(5, 20), false), (Point(20, 5), false), (Point(0,0), true), (Point(9, 4), true), (Point(10, 5), false))
    tests foreach {
      case (point, expected) => field.hasInBounds(point) should be(expected)
    }
  }
  
  it should "create the right number of bombs" in {
    val maxX = 10
    val maxY = 10
    val expected = Random.nextInt(19) + 1
    
    val bombs = Field.makeBombs(maxX, maxY, expected)
    
    bombs.size should be(expected)
  }
  
  it should "populate the field with the right number of bombs" in {
    val maxX = Random.nextInt(10) + 10
    val maxY = Random.nextInt(10) + 10
    val expected = Random.nextInt(19) + 1
    
    val field = Field(maxX, maxY, expected)
    
    field.numBombs should be(expected)
  }
  
  it should "detect a bomb" in {
    val field = Field(2, 1, Set(Point(1, 0)))
    
    field.hasBombAt(Point(0, 0)) should be(false)
    field.hasBombAt(Point(1, 0)) should be(true)
  }
  
  "Point" should "correctly handle addition" in {
    val x = Random.nextInt(10)
    val y = Random.nextInt(10)
    val dx = Random.nextInt(10) - 5
    val dy = Random.nextInt(10) - 5
    val expX = x + dx
    val expY = y + dy
    
    val p = Point(x, y) + (dx, dy)
    
    p should be(Point(expX, expY))
  }
  
  it should "correctly calculate locals" in {
    val point = Point(Random.nextInt(10), Random.nextInt(10))
    
    point.locals.length should be(8)
    val set = point.locals.toSet
    set.size should be(8)
    set.contains(point) should be(false)
    set.contains(Point(point.x - 1, point.y + 1)) should be(true)
  }
  
  it should "correctly calculate neighbours in midfield" in {
    implicit val field = Field(5, 5, 1)
    val point = Point(2, 2)
    
    point.neighbours.length should be(8)
    point.neighbours.toSet.contains(Point(3, 3)) should be(true)
  }
  
  it should "correctly calculate neighbours on an edge or corner" in {
    implicit val field = Field(3, 3, 1)
    val point = Point(0, 0)
    
    point.neighbours.length should be(3)
    val set = point.neighbours.toSet
    set.size should be(3)
    set.contains(Point(1, 1)) should be(true)
    set.contains(Point(-1, 0)) should be(false)
  }
  
  it should "correctly calculate distances" in {
    val point = Point(2, 2)
    val p1 = Point(3, 2)
    val p2 = Point(0, 2)
    val p3 = Point(2, 5)
    val p4 = Point(3, 3)
    val p5 = Point(5, 6)
    
    point - p1 should be(1)
    point - p2 should be(2)
    point - p3 should be(3)
    point - p4 should be(1)
    point - p5 should be(5)
  }
  
  "DisplayPoint" should "correctly mark a bomb" in {
    implicit val field = Field(3, 3, Set(Point(1, 1)))
    
    val dp = DisplayPoint(1, 1)
    dp.isBomb should be(true)
    dp shouldBe a[BombPoint]
  }
  
  it should "correctly mark a clear point" in {
    implicit val field = Field(3, 3, Set(Point(1, 1)))
    
    val dp = DisplayPoint(0, 0)
    dp.isBomb should be(false)
    dp shouldBe a[ClearPoint]
  }
  
  "FieldState" should "correctly determine a Lost state" in {
    implicit val field = Field(2, 2, Set(Point(1, 1)))
    val display = Map(Point(1, 1) -> DisplayPoint(1, 1))
    
    val fieldState = FieldState(display)
    
    fieldState.state should be(Lost)
  }
  
  it should "correctly determine a Won state" in {
    implicit val field = Field(2, 2, Set(Point(1, 1), Point(1, 0)))
    val display = Map(Point(0, 0) -> DisplayPoint(0, 0), Point(0, 1) -> DisplayPoint(0, 1))
    
    val fieldState = FieldState(display)
    
    fieldState.state should be(Won)
  }
  
  it should "correctly determine a Playing state" in {
    implicit val field = Field(2, 2, Set(Point(1, 1), Point(1, 0)))
    val display = Map(Point(0, 0) -> DisplayPoint(0, 0))
    
    val fieldState = FieldState(display)
    
    fieldState.state should be(Playing)
  }
  
  it should "play a game to its end" in {
    implicit val field = Field(2, 2, Set(Point(1, 1), Point(1, 0)))
    val fs0 = FieldState()
    fs0.state should be(Playing)
    
    val fs1 = fs0.touch(0, 0)
    fs1.state should be(Playing)
    
    val fs2 = fs1.touch(0, 1)
    fs2.state should be(Won)
    
    val fs3 = fs1.touch(1, 1)
    fs3.state should be(Lost)
  }
  
  it should "correctly manage a click on a zero point" in {
    implicit val field = Field(3, 3, Set(Point(2, 2)))
    
    val fs0 = FieldState()
    val fs1 = fs0.touch(0, 0)
    
    fs1.state should be(Won)
  }
  
  it should "correctly locate a zero point" in {
    implicit val field = Field(3, 3, Set(Point(2, 0), Point(2, 2), Point(0, 2)))
    field.zeros should be(Seq(Point(0,0)))
    
    val fs = FieldState()
    fs.zero() should be(Some(Point(0, 0)))
  }
}