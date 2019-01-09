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

import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization     := "com.bpgracey",
      organizationName := "Bancroft Gracey",
      developers       := List(
      	Developer(
      		id = "1",
      		name = "Bancroft P. Gracey", 
      		email = "bancroft.gracey@gmail.com",
      		url = new URL("https://www.linkedin.com/in/bancroftgracey/")
      	)),
      licenses         := List(
      	"GNU General Public License Version 3.0" -> new URL("http://www.gnu.org/licenses/gpl-3.0-standalone.html")
      	),
      description      := "A Minesweeper game engine (with refinements)",
      scalaVersion     := "2.12.6",
      version          := "0.2.0-SNAPSHOT"
    )),
    name := "Minefield",
    libraryDependencies += scalaTest % Test,
    Keys.`package` in Compile := (Keys.`package` in Compile).dependsOn(test in Test).value,
    Keys.`publish` in Compile := (Keys.`publish` in Compile).dependsOn(test in Test).value,
    
    
    publishTo := Some(Resolver.mavenLocal),
    publishMavenStyle := true,
    pomExtra :=
      <!--
    Copyright 2018 Bancroft P. Gracey  
      
    This file is part of Minefield.

    Minefield is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Minefield is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Minefield.  If not, see <https://www.gnu.org/licenses/>.
      -->

  )
