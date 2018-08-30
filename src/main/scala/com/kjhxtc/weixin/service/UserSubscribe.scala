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

import com.jfinal.weixin.sdk.api.UserApi
import com.kjhxtc.mwemxa.Logger
import com.kjhxtc.mwemxa.Model.WechatUser
import org.quartz.{Job, JobExecutionContext}

class UserSubscribe extends Job with Logger {
  override def execute(context: JobExecutionContext): Unit = {
    try {
      log debug context.getJobDetail.getJobDataMap.get("openid").asInstanceOf[String]
      log debug context.get("openid").asInstanceOf[String]
      val openid = context.getJobDetail.getJobDataMap.get("openid").asInstanceOf[String]
      val ghId = context.getJobDetail.getJobDataMap.get("ghId").asInstanceOf[String]
      log debug s"Fetching User $openid Information From WeChat Server ..."
      val status = UserApi.getUserInfo(openid)
      val dao = new WechatUser()
      if (status.isSucceed) {
        val row = dao
          .set("OPENID", status.getStr("openid"))
          .set("GH_ID", ghId)
          .set("NICK_NAME", status.getStr("nickname"))
          .set("SEX", status.getInt("sex"))
          .set("LANGUAGE", status.getStr("language"))
          .set("CITY", status.getStr("city"))
          .set("PROVINCE", status.getStr("province"))
          .set("COUNTRY", status.getStr("country"))
          .set("HEADIMGURL", status.getStr("headimgurl"))
          .set("SUBSCRIBE_TIME", status.getLong("subscribe_time"))
          .set("GROUPID", "" + status.getInt("groupid"))
          .set("TAGID_LIST", status.getList("tagid_list").toString)
          .set("SUBSCRIBE_SCENE", status.getStr("subscribe_scene"))
        //        .set("", status.getStr("qr_scene"))
        //        .set("", status.getStr("qr_scene_str"))
        Option(status.getStr("unionid")).foreach(uid => if (uid.nonEmpty) row.set("UNIONID", uid))
        Option(status.getStr("remark")).foreach(mark => if (mark.nonEmpty) row.set("REMARK", mark))
        row.save()
      } else {
        log error "" + status.isAccessTokenInvalid
        log error status.getErrorMsg
      }
      context.put("status", "ok")
    } catch {
      case ex: Throwable =>
        context.put("status", "failed")
        log error "自动添加异常,埋点手动添加 ..."
        ex.printStackTrace()
    }
  }
}
