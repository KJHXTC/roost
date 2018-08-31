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

import java.sql.SQLException

import com.jfinal.core.ActionKey
import com.jfinal.weixin.sdk.api._
import com.jfinal.weixin.sdk.jfinal.ApiController
import com.kjhxtc.mwemxa.Model.{WeChatGH, WechatUser}
import com.kjhxtc.mwemxa.{AppCache, Logger}

/**
  * 微信账户服务控制
  * 账号关联 通过微信登录 等
  */
class AdminController extends ApiController with Logger {

  def index(): Unit = {
    renderText("main page")
  }

  @ActionKey("/weixin/admin")
  def admin(): Unit = {
    val follows = UserApi.getFollows
    if (follows.getErrorCode != 0) {
      log error "Failed get user infos::" + follows.getErrorMsg
    }
    val sb = new StringBuilder()
    follows.getAttrs.forEach((a, b) =>
      sb.append(s"$a => ${b.toString}\n")
    )
    renderText(sb.toString())
  }

  @ActionKey("/weixin/connect")
  def connect(): Unit = {
    //微信(可能未关注公众号) 授权后

    //从SNS绑定跳转而来
    getHeader("Refer") match {
      case null =>
        log debug "从微信点击绑定认证而来"
        val k = getPara("state")
        val gh_id = AppCache.GlobalTempCache.asMap().get(k)
        if (gh_id != null && gh_id.length > 0) {
          val records = new WeChatGH().find("SELECT * FROM WECHAT_GH WHERE GH_ID=?", gh_id)
          if (records.size() != 1) throw new IllegalStateException("RECORD MUST BE 1")

          val code = getPara("code")
          if (code == null | code.length < 1) throw new IllegalArgumentException("oauth2 code Must be nonEmpty")
          val appId = records.get(0).getStr("appID")
          val appSecret = records.get(0).getStr("appSecret")
          val token = SnsAccessTokenApi.getSnsAccessToken(appId, appSecret, code)
          // 单就登录/绑定来讲 只需要知道openid即可
          if (token.getErrorCode == 0) {
            setSessionAttr("openid", token.getOpenid)
          }
        }
        // 用于绑定网站账号的微信账号标识
        render("connect.html")


      case string: String if string.startsWith("https://open.weixin.qq.com/connect/oauth2/authorize?") =>


      case others: String =>
        log error "nosi-->" + others
        renderError(401)
    }
    setAttr("title", "账号关联 - 微信 - 身份认证")
  }

  @ActionKey("/weixin/signup")
  def signup(): Unit = {
    render("signup.html")
  }

  @ActionKey("/weixin/bind")
  def bind(): Unit = {
    val session = getSession(false)
    if (null == session) {
      renderError(403)
    }
    //1. 获取对应的公众号 openid
    val openId: String = session.getAttribute("openid").asInstanceOf
    if (null == openId || openId.length < 1) {
      renderError(403)
    }
    //2. 获取认证通过的 网站用户标识 uid
    val uid: String = session.getAttribute("uid").asInstanceOf
    if (null == uid || uid.length < 1) {
      renderError(403)
    }

    //3. 建立绑定关系并写入持久化
    try {
      new WechatUser().dao().findOpenId(openId).getOrElse(throw NoSuchUserError("")).bindOpenIDwithUserID(uid.toLong)
    } catch {
      case sql: SQLException =>
        log error "SQL EXX" + sql.getMessage
      case th: Throwable =>
        th.printStackTrace()
    }

    //4. 返回成功/失败结果 页面渲染
    renderText("success")
  }
}

case class NoSuchUserError(msg: String) extends NoSuchFieldError(msg)