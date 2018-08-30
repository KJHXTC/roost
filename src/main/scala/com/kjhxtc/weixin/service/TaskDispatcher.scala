/*
 * Copyright (c) 2018 kjhxtc.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kjhxtc.weixin.service

import java.util.Date

import com.kjhxtc.mwemxa.Jobs
import org.quartz.{JobBuilder, TriggerBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object TaskDispatcher {

  def addUser(openId: String, ghId:String): Unit = {
    Future {
      val job = JobBuilder.newJob(classOf[UserSubscribe])
        .withIdentity(openId, "WeChatUser")
        .usingJobData("openid", openId)
        .usingJobData("ghId", ghId)
        .build
      import org.quartz.SimpleScheduleBuilder._

      val trigger = TriggerBuilder.newTrigger().withIdentity(openId, "WeChatUser")
        .startNow()
        .withSchedule(simpleSchedule())
        .build()
      Jobs.scheduler.scheduleJob(job, trigger)
    }
  }

  def preDeleteUser(openId: String): Unit = {
    val job = JobBuilder.newJob(classOf[UserUnSub])
      .withIdentity("job1", "WeChatUser")
      .usingJobData("openid", openId)
      .build
    import org.quartz.SimpleScheduleBuilder._

    val trigger = TriggerBuilder.newTrigger().withIdentity("xxx", "WeChatUser")
      .startAt(new Date(24 * 3600 * 1000 + System.currentTimeMillis()))
      //          .startAt(new Date(10 * 1000 + System.currentTimeMillis()))
      .withSchedule(simpleSchedule())
      .build()
    Jobs.scheduler.scheduleJob(job, trigger)
  }


}
