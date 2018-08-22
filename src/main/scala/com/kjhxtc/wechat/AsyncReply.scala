package com.kjhxtc.wechat

import java.net.SocketException

import com.jfinal.weixin.sdk.msg.out._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util._


class AsyncReply {

  // 在多数业务比较繁忙或者 耗时较长的场景中,使用异步应答来解决
  // 即调用微信的客服接口 主动向微信服务器 PUSH 应答消息
  // 但需要您的 公众号通过认证(每年一次,每次 300RMB的认证费用)
  import com.jfinal.weixin.sdk.api._

  def reply(msg: OutMsg): Unit = Future {
    msg match {
      case out: OutTextMsg =>
        CustomServiceApi.sendText(out.getFromUserName, out.getContent)

      case out: OutImageMsg =>
        CustomServiceApi.sendImage(out.getFromUserName, out.getMediaId

      case out: OutMusicMsg =>
        CustomServiceApi.sendMusic(out.getFromUserName, out.getMusicUrl, out.getHqMusicUrl,
          "", out.getTitle, out.getDescription) // TODO  原OutMusicMsg不支持Thumb_Media_Id

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

      } else {
        // Notify other Handlers 一般为不支持
        // MessageQueue.toggle(user.msgId, )
      }


    /**
      * 应答消息 是主动向 微信服务器 PUSH 结果
      * 可能遇到 网络问题 等待 一定时间后重试即可
      */
    case Failure(exception) =>
      exception match {
        case ex: SocketException =>


      }
    // MessageQueue.Failed(user.msgId, )

  }
}


