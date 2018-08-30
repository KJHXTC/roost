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

import java.net.SocketException

import com.jfinal.weixin.sdk.api._
import com.jfinal.weixin.sdk.msg.out._
import com.kjhxtc.mwemxa.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util._

/**
  * 在多数业务比较繁忙或者 耗时较长的场景中,使用异步应答来解决
  * 即调用微信的客服接口 主动向微信服务器 PUSH 应答消息
  * 但需要您的 公众号通过认证(每年一次,每次 300RMB的认证费用)
  */
object AsyncReply extends Logger {


  /**
    * 处理常见的消息格式包括
    * <ul>
    * <li>OutTextMsg</li>
    * <li>OutImageMsg</li>
    * <li>OutMusicMsg</li>
    * <li>OutVoiceMsg</li>
    * <li>OutVideoMsg</li>
    * <li>OutNewsMsg</li>
    * </ul>
    *
    * @param msg 原及时响应的应答消息
    */
  def reply(msg: OutMsg): Unit = Future {
    msg match {
      case out: OutTextMsg =>
        CustomServiceApi.sendText(out.getFromUserName, out.getContent)

      case out: OutImageMsg =>
        CustomServiceApi.sendImage(out.getFromUserName, out.getMediaId)

      case out: OutMusicMsg =>
        CustomServiceApi.sendMusic(out.getFromUserName, out.getMusicUrl, out.getHqMusicUrl,
          "", out.getTitle, out.getDescription) // TODO  原OutMusicMsg中 不支持 Thumb_Media_Id

      case out: OutVideoMsg =>
        CustomServiceApi.sendVideo(out.getFromUserName, out.getMediaId, out.getTitle, out.getDescription)

      case out: OutVoiceMsg =>
        CustomServiceApi.sendVoice(out.getFromUserName, out.getMediaId)

      case out: OutNewsMsg =>
        val articles = new java.util.ArrayList[CustomServiceApi.Articles]()
        out.getArticles.forEach(n => {
          val article = new CustomServiceApi.Articles()
          article.setTitle(n.getTitle)
          article.setDescription(n.getDescription)
          article.setPicurl(n.getPicUrl)
          article.setUrl(n.getUrl)
          articles.add(article)
        })
        CustomServiceApi.sendNews(out.getFromUserName, articles)
    }
  } onComplete {
    case Success(stats) =>
      if (stats.isSucceed) {
        // MessageQueue.success(user.msgId, )
        log debug "Send success (Reply)"
      } else {
        // Notify other Handlers 一般为不支持
        // MessageQueue.toggle(user.msgId, )
        log debug "Send Failed (Reply)" + stats.getErrorMsg
      }


    /**
      * 应答消息 是主动向 微信服务器 PUSH 结果
      * 可能遇到 网络问题 等待 一定时间后重试即可
      */
    case Failure(exception) =>
      exception match {
        case ex: SocketException =>
          ex.printStackTrace()

      }
    // MessageQueue.Failed(user.msgId, )

  }
}


