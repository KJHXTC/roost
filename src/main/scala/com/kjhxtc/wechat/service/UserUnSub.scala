package com.kjhxtc.wechat.service

import com.kjhxtc.mwemxa.Logger
import com.kjhxtc.mwemxa.Model.WechatUser
import org.quartz.JobExecutionContext

class UserUnSub extends org.quartz.Job with Logger {
  override def execute(context: JobExecutionContext): Unit = {
    // Weixin
    val wx = new WechatUser
    log debug "Remove User From DB"
    val openid = context.get("openid").asInstanceOf[String]
    wx.findOpenId(openid) foreach (o => o.delete())
    context.put("openid", openid)
    context.put("status", "ok")
  }
}
