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

import com.kjhxtc.mwemxa.Logger
import com.kjhxtc.mwemxa.Model.WechatUser
import org.quartz.JobExecutionContext

class UserUnSub extends org.quartz.Job with Logger {
  override def execute(context: JobExecutionContext): Unit = {
    // Weixin
    val wx = new WechatUser
    log debug "Remove User From DB"
    val openid = context.getJobDetail.getJobDataMap.get("openid").asInstanceOf[String]
    wx.findOpenId(openid) foreach {
      o => if (o.delete()) context.put("status", "ok")
    }
  }
}
