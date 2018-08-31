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

package com.kjhxtc.webBrowser

import com.jfinal.aop.Before
import com.jfinal.ext.interceptor.Restful
import com.kjhxtc.mwemxa.Model.User
import com.kjhxtc.security.{AuthenticateHelper, PasswordMissMatchException}
import javax.servlet.http.Cookie

sealed abstract class LoginType

case object EmailLogin extends LoginType

case object PhoneLogin extends LoginType

case object NameLogin extends LoginType

@Before(Array {
  classOf[Restful]
})
class Signin extends MainController {

  override def onGet(): Unit = {
    val session = getSession(true)
    if (getCookie("BS_UUID") == null)
      setCookie("BS_UUID", randomData(32), Int.MaxValue)
    val rating = session.getAttribute("AuthRating")
    if (rating != null && rating.asInstanceOf[Int] > 2) {
      render("index.html")
    } else {
      val token = randomData(32)
      val timestamp = System.currentTimeMillis() / 1000
      // 用户认证时密码签名元素的一个动态因子
      // 时间戳 服务端单独存储使用,同时下传
      setSessionAttr("SERVER_RANDOM", token)
      setSessionAttr("SERVER_TIMESTAMP", timestamp.toString)
      setAttr("SERVER_RANDOM", token) //Session 临时存储
      setAttr("SERVER_TIMESTAMP", timestamp.toString)
      setAttr("SNS", "微信")
      render("signin.html")
    }
  }

  override def onPost(): Unit = {
    log info getHeader("Refer")
    if (getSession(false) == null) throw ForbiddenException("空会话")
    if (getCookie("BS_UUID") == null) throw ForbiddenException("无访问标识")
    val logins: (LoginType, String) = getPara("signin_username") match {
      case REG_EMAIL(email) => (EmailLogin, email)
      case REG_LOGIN(name) => (NameLogin, name)
      case REG_Phone(cn, id) => (PhoneLogin, "+%s %s" format(cn, id))
      case _ =>
        log error "Login Type unknown"
        throw BadRequestException("Invalid login id")
    }

    // 登录处理 客户端元素(`客户端随机数`,`客户端时间戳`,`时间差`)
    // 服务端从 Session 中获取 `服务端随机数`,`服务端时间戳`
    // 认证元素 hash(`服务端随机数`,`客户端随机数`),`服务端时间戳`,`客户端时间戳`,`时间差`
    val sr = getSessionAttr("SERVER_RANDOM").asInstanceOf[String]
    val st = getSessionAttr("SERVER_TIMESTAMP").asInstanceOf[String]
    val cr = getPara("__LOGIN_TOKEN__")
    val ct = getPara("__LOGIN_TOKEN_TIMESTAMP__")
    val dt = getPara("__LOGIN_DELTA_TIMESTAMP__")
    val signature = getPara("signature")
    if (sr == null || st == null) {
      log error "服务器 无记录"
      throw ForbiddenException("NO RECORD OR CSRF")
    }
    if (cr == null || ct == null || dt == null) {
      log error "客户端 缺少认证参数"
      throw BadRequestException("Param ERROR")
    }
    log debug sr
    log debug cr
    log debug st
    log debug ct
    log debug dt
    val token: Array[Byte] = decode(hash(cr, sr)) ++ st.getBytes ++ ct.getBytes ++ dt.getBytes
    removeSessionAttr("SERVER_RANDOM")
    removeSessionAttr("SERVER_TIMESTAMP")
    val u = new User()
    val found = logins._1 match {
      case EmailLogin =>
        u.findByEmail(logins._2)
      case NameLogin =>
        u.findByLogin(logins._2)
    }
    if (found.isEmpty) {
      throw AuthenticateException(s"User [${logins._2}] Not Exists!")
    }

    try {
      AuthenticateHelper().verifyPassword(decode(signature), token, found.get.getLong("ID").toString,
        AuthenticateHelper.testKey, found.get.getStr("PASSWORD"))
    } catch {
      case _: PasswordMissMatchException =>
        throw AuthenticateException("认证失败")
    }
    val cookie = new Cookie("LOGIN", found.get.getStr("LOGIN"))
    cookie.setMaxAge(7 * 24 * 3600)
    cookie.setSecure(true)
    setCookie(cookie)
    render("index.html")
  }
}
