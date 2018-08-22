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

package com.kjhxtc.wechat

import com.jfinal.weixin.sdk.jfinal.MsgControllerAdapter
import com.jfinal.weixin.sdk.msg.in._
import com.jfinal.weixin.sdk.msg.in.event._
import com.jfinal.weixin.sdk.msg.out._
import com.kjhxtc.mwemxa.Logger
import com.kjhxtc.mwemxa.Model.WechatUser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, TimeoutException}
import scala.util._


class WeChatController extends MsgControllerAdapter with Logger{
  val wxuser = new WechatUser()

  // 如果处理时间内未完成 则 响应一个 Success 否则响应结果
  def within(in: InMsg)(c: () => OutMsg): Unit = {
    val ct = System.currentTimeMillis()
    val f = Future[OutMsg](c())

    Await.ready(f, Duration.Inf) flatMap {
      value =>
        Future {
          val ce = System.currentTimeMillis()
          if (ce - ct > 4500) {
            render(value)
          } else {
            new AsyncReply().reply(in)
          }
        }
    }
  }

  override def processInTextMsg(inTextMsg: InTextMsg): Unit = {

    within(inTextMsg) {

      val f = wxuser.findOpenId(getPara("openid"))
      if (f.isEmpty) {
        log warn "notify user manager to get this openid to db"
        // todo
      }
      log debug inTextMsg.getMsgId

      inTextMsg.getContent match {
        case "绑定" =>
          val out = new OutNewsMsg(inTextMsg)
          val news = new News()
          news.setUrl("https://wms.kedyy.com/weixin/connect")
          news.setTitle("前往绑定账号 ...")
          news.setDescription("将此微信号与您的SITE 账号进行关联 享受更多便利")
          news.setPicUrl("https://wms.kedyy.com/assets/demo/pricing-bg.jpg")
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
          u.foreach(
            f => outMsg.setDescription(f.getStr("NICK_NAME"))
          )
          outMsg
      }


    }
  }


  override def processInFollowEvent(inFollowEvent: InFollowEvent): Unit = {
    val p = new OutTextMsg(inFollowEvent)

    if (inFollowEvent.getEvent.equalsIgnoreCase("subscribe")) {
      p.setContent(
        s"""欢迎关注 ${inFollowEvent.getToUserName}
           |还可以回复关键字 绑定 进行账号关联
           |想你所想, 如你所愿~
       """.stripMargin)
      log warn "notify user manager to get this openid to db"
      // todo
      render(p)

    } else {
      log warn "Notice:: User unsubscribe ..."
      log warn "nofify user manager to update openid to db"
      renderDefault()
    }
  }

  protected override def renderDefault() {
    renderText("Success")
  }

  override def processInMenuEvent(inMenuEvent: InMenuEvent): Unit = {
    log debug inMenuEvent.getEventKey
    renderDefault()
  }
}
