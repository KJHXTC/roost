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
import com.jfinal.core.Controller
import com.jfinal.ext.interceptor.Restful
import com.jfinal.plugin.activerecord.{Db, IAtom, Record}
import com.kjhxtc.mwemxa.Model.User
import com.kjhxtc.mwemxa.{ISHelper, Logger, snowflake}
import com.kjhxtc.security.policy.Rating
import com.kjhxtc.security.{AuthenticateHelper, OTPGene}
import javax.crypto.spec._
import javax.servlet.http.Cookie

@Before(Array {
  classOf[Restful]
})
class Signup extends Controller with Logger with ISHelper {
  /**
    * 注册页面只允许 HTTPS 方式访问(为了防止中间人攻击)
    * 当用户进行注册时,先在客户端产生随机ID
    * 使用HMac签名技术,在后端存储 key (一个随机密钥),并将客户端的标识和当前系统时间一起签名
    * 将 `签名值` 放置到 Cookie 中
    * 将 `CSRF_TOKEN` 和 `生成时间` 放置到 表单的 Hidden 元素中
    */
  def index(): Unit = {

    val clientId = getCookie("BS_UUID") //浏览器唯一标识 - Base64编码
    if (null == clientId) {
      renderError(403)
      return
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
  def save(): Unit = {
    // 安全过滤
    val clientId = getCookie("BS_UUID", "") //浏览器唯一标识 - Base64编码
    if (clientId.isEmpty) {
      renderError(403)
      throw new Exception()
    }
    val session = getSession(false)

    if (null == session || null == session.getAttribute("CSRF_TOKEN_KEY")) {
      renderError(403)
      throw new Exception()
    }

    // ONLY HTTPS
    val signature = getCookie("SIGNATURE", "")
    if (signature.isEmpty) {
      renderError(403)
      throw new Exception()
    }

    val ua = getHeader("User-Agent")
    log info ua
    if (!snowflake.validUA(ua)) {
      renderError(403)
      throw new Exception()
    }

    // 提取表单属性
    val csrf_time = getPara("__CSRF_TOKEN_TIME__")

    //验证表单安全性
    val token_key = new SecretKeySpec(decode(session.getAttribute("CSRF_TOKEN_KEY").asInstanceOf[String]), "HMacSHA256")
    val token = decode(clientId) ++ csrf_time.getBytes
    //这个签名数据是由后台系统独立计算,独立验证的,与前端无关
    //故如果验证失败则说明数据被篡改
    AuthenticateHelper().verifySignature(decode(signature), token, token_key)

    //检测会话时间差 如果偏移时间过大则设置提示然后刷新页面重新进行(可以缓存客户输入,提高使用友好度)
    if (System.currentTimeMillis() / 1000 - java.lang.Long.parseUnsignedLong(csrf_time) > 3600) {
      throw new Exception()
    }
    //
    val kv = decode(hash(clientId, session.getAttribute("CSRF_TOKEN_KEY").asInstanceOf[String]))
    val key = new SecretKeySpec(kv.slice(0, 16), "AES")
    val iv = new IvParameterSpec(kv.slice(16, 32))
    val userEmail = getPara("signup_email") match {
      case REG_EMAIL(email) => email
      case _ =>
        renderError(400, "Email Illegal")
        throw new Exception()
    }

    val userName = getPara("signup_username") match {
      case REG_LOGIN(uuid) => uuid
      case _ => renderError(400, "UserName Illegal")
        throw new Exception()
    }
    if (session.getAttribute("NONCE") != getPara("__NONCE__")) {
      throw new Exception()
    }
    session.removeAttribute("NONCE")

    val db = new User()
    val exists = db.findByLogin(userName.toString)
    if (exists.nonEmpty) {
      throw new Exception()
    }

    // 用户密码密文
    val cipher = decode(getPara("signup_password"))
    val uid = snowflake.getId(ua)
    val pass = try {
      AuthenticateHelper().makePassword(key, Some(iv), cipher, uid.toString, AuthenticateHelper.testKey)
    } catch {
      case e: Throwable =>
        throw e
    }
    // EMAIL DB
    db.dao().find("SELECT * FROM tb_email WHERE VALUE=?", userEmail.toUpperCase).forEach(x => {
      renderError(400)
      throw new Exception(s"EMAIL [${x.getStr("VALUE")}]REGISTERED")
    })
    val ok = Db.tx(new IAtom {
      override def run(): Boolean = {
        //开始 锁定
        val user = new Record()
          .set("ID", uid)
          .set("LOGIN", userName)
          .set("PASSWORD", pass)
          .set("TOKEN", OTPGene.randomSecure())
        val email = new Record()
          .set("UID", uid)
          .set("VALUE", userEmail.toUpperCase())
          .set("RATING", Rating.REG_EMAIL)

        Db.save("USER", user) && Db.save("TB_EMAIL", email)
        //结束 锁定
      }
    })
    // TODO 人为操作记录
    // op.log(time, ip, uid, ACTION.REGISTER, SUCCESS, ua, this.class.name)
    if (ok) {
      redirect("/signin")
    } else {
      renderError(500)
    }
  }
}
