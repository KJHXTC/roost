package com.kjhxtc.mwemxa

import com.jfinal.log.Log

trait Logger {
  val log: Log = Log.getLog(this.getClass)
}
