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

package com.kjhxtc.weixin

import com.jfinal.weixin.sdk.api.SnsAccessTokenApi
import com.jfinal.weixin.sdk.jfinal.MsgControllerAdapter
import com.jfinal.weixin.sdk.msg.in._
import com.jfinal.weixin.sdk.msg.in.event._
import com.jfinal.weixin.sdk.msg.out._
import com.kjhxtc.mwemxa.Model.WechatUser
import com.kjhxtc.mwemxa.{AppCache, ISHelper, Logger}
import com.kjhxtc.weixin.service.{AsyncReply, TaskDispatcher}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, TimeoutException}


class WeixinController extends MsgControllerAdapter with Logger {
  val wxuser = new WechatUser()

  /**
    * 一般短耗时的任务 (微信设定为 5秒内可响应的)
    * 这里推荐 3秒内(实际设定为4S)可响应的 执行同步应答
    * 否则执行异步应答(要求公众号通过认证,否则应答失败)
    */
  def within(c: => OutMsg)(implicit in: InMsg): Unit = {
    var async = false
    val task = Future {
      val value = c
      //即使超时 也会继续执行 除非这里出现异常
      if (async) {
        log debug "通过客服应答接口回复用户请求"
        AsyncReply.reply(value)
      } else {
        // 被动回复用户消息 https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421140543
        log debug "直接应答用户请求"
        render(value)
      }
    }

    try {
      log debug "任务处理中 ..."
      Await.result(task, FiniteDuration(4, SECONDS))
      // 如果未超时 则继续向下执行
    } catch {
      case _: TimeoutException =>
        async = true
        log debug "Task take more than 3000ms, reply will set async"
        renderDefault()
      case th: Throwable =>
        val error = new OutTextMsg(in)
        error.setContent("服务器故障.您的请求已记录,我们会在修正后给予答复,感谢您的支持!")
        render(error)
        th.printStackTrace()
      // 监控埋点
    }
  }

  override def renderDefault() {
    renderText("Success")
  }

  override def processInTextMsg(inTextMsg: InTextMsg): Unit = {
    within {
      val f = wxuser.findOpenId(getPara("openid"))
      if (f.isEmpty) {
        log warn "notify user manager to get this openid to db"
        TaskDispatcher.addUser(inTextMsg.getFromUserName, inTextMsg.getToUserName)
      }

      log debug inTextMsg.getMsgId

      inTextMsg.getContent match {

        case "绑定" =>
          // 根据公众号状态进行绑定
          // 已录入的公众号
          // 1. 用于开发的订阅号
          // 2. 用于正式的订阅号(未认证) API 受限
          // 3. 用于正式的订阅号        仅限订阅的API
          // 4. 用于开发的服务号
          // 5. 用于正式的服务号(未认证) API 受限
          // 6. 用于正式的服务号        仅限服务的API
          // 这里为了方便统一,仅使用 sns_base 来获取用户的openid即可
          val out = new OutNewsMsg(inTextMsg)
          val news = new News()
          val host = getRequest.getServerName
          val r = wxuser.find("SELECT * FROM WX_GH WHERE GH_ID=?", inTextMsg.getToUserName)
          if (r.size() == 1 && Option(r.get(0).getStr("appID")).nonEmpty) {
            val ticket = ISHelper.unionTicket
            AppCache.GlobalTempCache.put(ticket, inTextMsg.getToUserName)
            val url = SnsAccessTokenApi.getAuthorizeURL(
              r.get(0).getStr("appID"),
              java.net.URLEncoder.encode(s"https://$host/weixin/connect", "UTF-8"),
              ticket,
              true)
            news.setUrl(url)
            news.setTitle("前往绑定账号 ...")
            news.setDescription("将此微信号与网站账号进行关联 享受更多便利")
            news.setPicUrl("https://wms.kjhxtc.com/assets/demo/pricing-bg.jpg")
            out.addNews(news)
          }
          else {
            //暂时无法进行SNS绑定 (无法获取公众号对应的 appID以及appSecret)
            news.setUrl(s"https://$host/weixin/connect")
            news.setTitle("前往绑定账号 ...")
            news.setDescription("将此微信号与网站账号进行关联 享受更多便利")
            news.setPicUrl("https://wms.kjhxtc.com/assets/demo/pricing-bg.jpg")
            out.addNews(news)
          }
          out

        case "帮助"
        =>
          val out = new OutNewsMsg(inTextMsg)
          val news = new News()
          val host = getRequest.getServerName
          news.setUrl(s"https://$host/weixin/help")
          news.setTitle("使用手册")
          news.setDescription("点此获取使用说明手册")
          news.setPicUrl("https://wms.kjhxtc.com/assets/demo/pricing-bg.jpg")
          out.addNews(news)
          out

        case _ =>
          val outMsg = new OutMusicMsg(inTextMsg)
          outMsg.setTitle("Day By Day")
          outMsg.setDescription("建议在 WIFI 环境下流畅欣赏此音乐")
          outMsg.setMusicUrl("http://www.jfinal.com/DayByDay-T-ara.mp3")
          outMsg.setHqMusicUrl("http://www.jfinal.com/DayByDay-T-ara.mp3")
          outMsg.setFuncFlag(true)
          val u = wxuser.findOpenId(inTextMsg.getFromUserName)
          u foreach {
            f => outMsg.setDescription(f.getStr("NICK_NAME"))
          }
          outMsg
      }
    }(inTextMsg)
  }


  override def processInFollowEvent(inFollowEvent: InFollowEvent): Unit = {
    val p = new OutTextMsg(inFollowEvent)

    inFollowEvent.getEvent match {
      case InFollowEvent.EVENT_INFOLLOW_SUBSCRIBE =>
        p.setContent(
          s"""欢迎关注 ${
            inFollowEvent.getToUserName
          }
             |回复关键字 绑定 进行账号关联
             |回复关键字 帮助 查看更多玩法
             |想你所想, 如你所愿, 祝你开心每一天~
       """.stripMargin)
        log warn "Notify user manager to get this openid to db"
        // todo
        TaskDispatcher.addUser(inFollowEvent.getFromUserName, inFollowEvent.getToUserName)
        render(p)

      case InFollowEvent.EVENT_INFOLLOW_UNSUBSCRIBE =>
        log warn "Notice:: User unsubscribe ..."
        log warn "notify user manager to update openid to db"
        renderDefault()
        TaskDispatcher.preDeleteUser(inFollowEvent.getFromUserName)
    }
  }

  override def processInShortVideoMsg(inShortVideoMsg: InShortVideoMsg): Unit = {
    log debug inShortVideoMsg.getMediaId
  }

  override def processInVideoMsg(inVideoMsg: InVideoMsg): Unit = {
    log debug inVideoMsg.getThumbMediaId
  }

  override def processInVoiceMsg(inVoiceMsg: InVoiceMsg): Unit = {
    log debug inVoiceMsg.getFormat
  }

  override def processInImageMsg(inImageMsg: InImageMsg): Unit = {
    log debug inImageMsg.getPicUrl
  }

  override def processInLinkMsg(inLinkMsg: InLinkMsg): Unit = {
    log debug inLinkMsg.getUrl
  }

  override def processInLocationMsg(inLocationMsg: InLocationMsg): Unit = {
    log debug inLocationMsg.getLabel
    log debug inLocationMsg.getScale
  }

  override def processInLocationEvent(inLocationEvent: InLocationEvent): Unit = {
    log debug inLocationEvent.getPrecision
  }

  override def processInMenuEvent(inMenuEvent: InMenuEvent): Unit = {
    log debug inMenuEvent.getEventKey
    log debug inMenuEvent.getEvent


    inMenuEvent.getEvent match {
      case InMenuEvent.EVENT_INMENU_CLICK =>

      case InMenuEvent.EVENT_INMENU_VIEW =>
      case InMenuEvent.EVENT_INMENU_VIEW_LIMITED =>
      case InMenuEvent.EVENT_INMENU_MEDIA_ID =>
      case InMenuEvent.EVENT_INMENU_LOCATION_SELECT =>
        // WILL NOT HAPPEN
        log error "JFinal WEIXIN API ERROR, should be #processInLocationEvent"
      case InMenuEvent.EVENT_INMENU_SCANCODE_PUSH =>
      case InMenuEvent.EVENT_INMENU_scancode_waitmsg =>
      case InMenuEvent.EVENT_INMENU_PIC_PHOTO_OR_ALBUM =>
      case InMenuEvent.EVENT_INMENU_PIC_SYSPHOTO =>
      case InMenuEvent.EVENT_INMENU_PIC_WEIXIN =>
      case _ =>
    }
    renderDefault()
  }

  override def processInCustomEvent(inCustomEvent: InCustomEvent): Unit = {
    log debug inCustomEvent.getKfAccount
    log debug inCustomEvent.getToKfAccount
  }

  override def processInQrCodeEvent(inQrCodeEvent: InQrCodeEvent): Unit = {
    log debug inQrCodeEvent.getTicket
  }


}
