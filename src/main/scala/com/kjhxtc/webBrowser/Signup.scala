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
import com.jfinal.plugin.activerecord.{Db, IAtom, Record}
import com.kjhxtc.mwemxa.Model.User
import com.kjhxtc.mwemxa.snowflake
import com.kjhxtc.security.policy.{Rating, auditRecord}
import com.kjhxtc.security.{AuthenticateHelper, OTPGene}
import javax.crypto.spec._
import javax.servlet.http.Cookie

@Before(Array {
  classOf[Restful]
})
class Signup extends MainController {
  /**
    * 注册页面只允许 HTTPS 方式访问(为了防止中间人攻击)
    * 当用户进行注册时,先在客户端产生随机ID
    * 使用HMac签名技术,在后端存储 key (一个随机密钥),并将客户端的标识和当前系统时间一起签名
    * 将 `签名值` 放置到 Cookie 中
    * 将 `CSRF_TOKEN` 和 `生成时间` 放置到 表单的 Hidden 元素中
    */
  override def onGet(): Unit = {

    val clientId = getCookie("BS_UUID") //浏览器唯一标识 - Base64编码
    if (null == clientId) {
      //禁止 直接到注册页进行账号注册
      throw ForbiddenException("Forbidden Refer From:" + getHeader("Refer"))
    }
    val session = getSession(true)
    session.getAttribute("LOGIN_LEVEL") match {
      case null =>
      case x: String =>
    }
    val time = (System.currentTimeMillis() / 1000).toString
    val buff = randomData(32)
    val key = new SecretKeySpec(decode(buff), "HMacSHA256")
    // 签名元素包括 `浏览器的UUID` `时间戳`
    val data = decode(clientId) ++ time.getBytes
    val signatureValue = AuthenticateHelper().makeSignature(data, key)

    //设置页面
    val nonce = randomData(8)
    setAttr("CIPHER_KEY", hash(clientId, buff)) //不随表单再次提交
    setAttr("__CSRF_TOKEN_TIME__", time)
    setAttr("__NONCE__", nonce)
    setAttr("SNS", "微信")
    //设置Cookie
    val cookie = new Cookie("SIGNATURE", signatureValue)
    cookie.setHttpOnly(true)
    cookie.setMaxAge(-1)
    cookie.setPath("/signup")
    cookie.setSecure(true)
    setCookie(cookie)
    //设置Session
    session.setAttribute("CSRF_TOKEN_KEY", buff)
    session.setAttribute("NONCE", nonce)
    //页面渲染
    render("signup.html")
  }


  /**
    * 当用户进行注册提交时
    * 由浏览器自动将 Cookie 的 `签名值` 提交, 同时提交表单数据以及附加的安全属性值
    * `CSRF_TOKEN` `生成时间`
    * 使用HMac签名技术,在后端存储 key (一个随机密钥),并将客户端的标识和当前系统时间一起签名
    *
    * Session 会根据SessionManager中设定其特定Cookie后反射出原 Session
    * 使用密码加密是为了防止运维人员通过日志等方式接触到用户的明文密码
    */
  override def onPost(): Unit = {
    // 安全过滤
    log debug getRequest.getRemoteHost //FQDN or IP
    log debug getRequest.getRemoteAddr //IP
    log debug getHeader("X-Real-IP") //nginx apache
    log debug getHeader("X-Forwarded-For") //Squid
    val peerAddress = "%s|%s" format(getRequest.getRemoteAddr, getHeader("X-Real-IP"))
    // 过滤跨站点POST 如果大企业 有WAF 开发省事  - -
    log debug getHeader("Refer") // TODO 鉴别来源是不是注册页

    val ua = getHeader("User-Agent")
    log info ua
    if (!snowflake.validUA(ua)) {
      throw ForbiddenException("禁止无效浏览器标识访问")
    }
    val clientId = getCookie("BS_UUID", "") //浏览器唯一标识 - Base64编码
    if (clientId.isEmpty) {
      throw ForbiddenException("禁止直接访问注册页面")
    }
    val session = getSession(false)

    if (null == session || null == session.getAttribute("CSRF_TOKEN_KEY")) {
      throw ForbiddenException("空会话记录")
    }

    // ONLY HTTPS
    val signature = getCookie("SIGNATURE", "")
    if (signature.isEmpty) {
      throw ForbiddenException("无法来源鉴别身份")
    }

    // 提取表单属性
    val csrf_time = getPara("__CSRF_TOKEN_TIME__", "")
    //验证表单安全性
    val token_key = new SecretKeySpec(decode(session.getAttribute("CSRF_TOKEN_KEY").asInstanceOf[String]), "HMacSHA256")
    val token = decode(clientId) ++ csrf_time.getBytes
    //这个签名数据是由后台系统独立计算,独立验证的,与前端无关
    //故如果验证失败则说明数据被篡改
    AuthenticateHelper().verifySignature(decode(signature), token, token_key)

    //检测会话时间差 如果偏移时间过大则设置提示然后刷新页面重新进行(可以缓存客户输入,提高使用友好度)
    if (System.currentTimeMillis() / 1000 - java.lang.Long.parseUnsignedLong(csrf_time) > 3600) {
      index()
      return
    }
    // 防止用户POST数据被重放
    try {
      //重载页面,让用户再次提交, 可能是CSRF问题 也可能是 用户页面未刷新造成的差异(重复POST,重放)
      if (session.getAttribute("NONCE").asInstanceOf[String] != getPara("__NONCE__")) {
        index()
        return
      }
    } finally {
      session.removeAttribute("NONCE") //刷新时 会自动更新 NONCE
    }
    // 前端加密客户密码的密钥
    val kv = decode(hash(clientId, session.getAttribute("CSRF_TOKEN_KEY").asInstanceOf[String]))
    val key = new SecretKeySpec(kv.slice(0, 16), "AES")
    val iv = new IvParameterSpec(kv.slice(16, 32))
    // 页面是通过EMAIL渠道注册的,因此 EMAIL 必填
    // TODO 在本页直接加上 EMAIL 验证码
    // 过去多数网站都是注册后点击激活连接的方式来确认EMAIL地址的 我们采用类似于短信验证码方式
    // 不采用`短信验证码`是因为成本问题 对于非营利或者个人来讲有实际的负担
    // 中国政府(工信部)要求后台实名认证(手机号认证),无形中已经加重负担 但各种供应商接口不一 需要自行客户化
    // 如果采用微信/微博/支付宝这种认证登录, 也算变相的实名认证
    val userEmail = getPara("signup_email") match {
      case REG_EMAIL(s_email) => s_email
      case _ =>
        throw BadRequestException("Email Illegal")
    }

    val userName = getPara("signup_username") match {
      case REG_LOGIN(uuid) => uuid
      case _ =>
        throw BadRequestException("UserName Illegal")
    }

    // 粗略检测数据是否重复
    val db = new User()
    db.findByLogin(userName).foreach(x => {
      throw BadRequestException(s"LoginName [${x.getStr("LOGIN")}]REGISTERED")
    })
    // EMAIL DB
    db.dao().find("SELECT * FROM TB_EMAIL WHERE VALUE=?", userEmail.toUpperCase).forEach(x => {
      throw BadRequestException(s"EMAIL [${x.getStr("VALUE")}]REGISTERED")
    })

    // 用户密码密文
    val cipher = decode(getPara("signup_password"))
    val uid = snowflake.getId(ua)
    val pass = try {
      AuthenticateHelper().makePassword(key, Some(iv), cipher, uid.toString, AuthenticateHelper.testKey)
    } catch {
      case e: Throwable =>
        // 可能1. 解密用户密码异常 (客户端加密算法问题例如计算流程问题,密钥问题等),或者被攻击了
        // 可能2. 后台加密模块异常 例如模块无法正常工作
        throw e
    }
    val time = new java.util.Date
    val user = new Record()
      .set("ID", uid)
      .set("LOGIN", userName)
      .set("PASSWORD", pass)
      .set("TOKEN", OTPGene.randomSecure())
    val email = new Record()
      .set("UID", uid)
      .set("VALUE", userEmail.toUpperCase())
      .set("RATING", Rating.REG_EMAIL)
    val ok = Db.tx(new IAtom {
      override def run(): Boolean = {
        //开始 锁定
        Db.save("USER", user) && Db.save("TB_EMAIL", email)
        //结束 锁定
      }
    })
    if (ok) {
      //人为操作 记录到审计日志中
      auditRecord.record(time, peerAddress, uid, userName, ua, auditRecord.REGISTER.toString, "REGISTERED", "WR#998302348273423")
      redirect("/signin")
    } else {
      index()
    }
  }
}
