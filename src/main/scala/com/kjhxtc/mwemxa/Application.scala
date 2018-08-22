package com.kjhxtc.mwemxa

import com.jfinal.core.JFinal
import com.jfinal.kit.PropKit
import com.jfinal.log.Log

object Application extends App with Logger {
  log debug "Start ..."
  PropKit.use("app.properties")
  val port = PropKit.getInt("port")
  JFinal.start(getClass.getResource("/").getPath, port, "/")
  log warn "main thread exit ...."
}
