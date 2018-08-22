import sbt._

name := "Bijeres"

version := "0.1"

scalaVersion := "2.12.0"

// https://mvnrepository.com/artifact/com.jfinal/jfinal
libraryDependencies += "com.jfinal" % "jfinal" % "3.4"
libraryDependencies += "com.jfinal" % "jetty-server" % "8.1.8"
libraryDependencies += "com.jfinal" % "jfinal-weixin" % "2.1"
// https://mvnrepository.com/artifact/log4j/log4j
libraryDependencies += "log4j" % "log4j" % "1.2.17"
// https://mvnrepository.com/artifact/commons-logging/commons-logging
libraryDependencies += "commons-logging" % "commons-logging" % "1.2"
libraryDependencies += "net.liftweb" %% "lift-json" % "3.3.0"
// https://mvnrepository.com/artifact/mysql/mysql-connector-java
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.46"
// https://mvnrepository.com/artifact/com.mchange/c3p0
libraryDependencies += "com.mchange" % "c3p0" % "0.9.5"

// https://mvnrepository.com/artifact/org.freemarker/freemarker
libraryDependencies += "org.freemarker" % "freemarker" % "2.3.23"
//libraryDependencies += "com.kjhxtc" % "BocLib" % "2.0.0-dev"

// https://mvnrepository.com/artifact/com.alibaba/fastjson
libraryDependencies += "com.alibaba" % "fastjson" % "1.2.49"
libraryDependencies += "org.jboss.aerogear" % "aerogear-otp-java" % "1.0.0"
// https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk15on
libraryDependencies += "org.bouncycastle" % "bcprov-jdk15on" % "1.60"
