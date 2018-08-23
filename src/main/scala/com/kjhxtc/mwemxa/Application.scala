package com.kjhxtc.mwemxa

import com.jfinal.core.JFinal
import com.jfinal.kit.PropKit

object Application extends App with Logger {
  log debug "Start ..."
  PropKit.use("app.properties")
  val port = PropKit.getInt("port")
  val agentID = PropKit.getInt("agentId")
  val clusterID = PropKit.getInt("clusterId")
  snowflake.init(agentID, clusterID)
  JFinal.start(getClass.getResource("/").getPath, port, "/")
  log warn "main thread exit ...."
}
