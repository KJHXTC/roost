package com.kjhxtc.mwemxa

import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory

object Jobs {
  val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler
}
