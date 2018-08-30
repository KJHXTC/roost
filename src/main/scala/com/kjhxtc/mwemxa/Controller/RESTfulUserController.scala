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

package com.kjhxtc.mwemxa.Controller

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.kjhxtc.mwemxa.Model.User
import com.kjhxtc.mwemxa.{ISHelper, Logger, NotifyCenter}
import com.kjhxtc.security.OTPGene
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonAST._

sealed case class RESTfulException(code: Int, message: String, map: Map[String, String] = Map()) extends Exception(message)


/**
  * 无 Session 模式
  *
  * RESTFul 中如何对客户认证状态评级?
  */
class RESTfulUserController extends JsonServiceCtrl with Logger with ISHelper {

  override def data: JsonAST.JValue = {
    someX()
  }

  def someX(): JsonAST.JValue = {
    try {
      // For Secure delay 1 second
      getPara("action") match {
        /**
          * 用于身份缓存
          */
        case "account" =>
          val u = new User()
          val login: Option[User] = getPara("name") match {
            case REG_EMAIL(email) =>
              u.findByEmail(email)
            case REG_LOGIN(name) =>
              u.findByLogin(name)
            case REG_Phone(cc, cn) =>
              u.findByPhone(cc, cn)
            case _ =>
              throw RESTfulException(400, "无效的用户名")
          }
          //客户端 挑战码
          val clt = Option[String](getPara("salt")).getOrElse(throw RESTfulException(400, "参数错误"))
          //服务端 应答码
          val svc = randomData(24)

          //计算客户端挑战凭据并缓存
          val challenge = hash(clt, svc)
          //缓存账号信息
          UserCache.set(challenge, login.getOrElse(throw RESTfulException(404, "无效的用户名")))
          //返回结果
          JObject(
            JField("status", JInt(200)),
            JField("salt", JString(svc))
          )

        /**
          * 密码认证,基于HMac方式的身份鉴别
          * 使用 用户口令 对 (身份标识 用途标识 UTC时间 UTC时间差) 进行计算
          */
        case "authorize" =>
          val signature = getPara("signature")
          val challenge = getPara("token") // 客户端计算的挑战凭据
          if (null == challenge || UserCache.get(challenge).isEmpty) {
            throw RESTfulException(403, "请重新登录")
          }
          val u = UserCache.get(challenge).get
          var c: List[JString] = Nil
          if (!u.verification(signature, challenge)) {
            throw RESTfulException(401, "认证失败,请检查登录名和密码")
          } else {
            if (u.hasEmail) c ++= List(JString("EMAIL"))
            if (u.hasPhone) c ++= List(JString("SMS"))
            if (u.has2FA) c ++= List(JString("Authenticator"))
          }

          if (c == Nil) {
            JObject(
              JField("status", JInt(200)),
              JField("enable2fa", JBool(false))
            )
          } else {
            JObject(
              JField("status", JInt(200)),
              JField("enable2fa", JBool(true)),
              JField("values", JArray(c))
            )
          }

        case "send2code" =>
          val token_id = getPara("token")
          if (null == token_id || UserCache.get(token_id).isEmpty) {
            throw RESTfulException(403, "请重新登录")
          }

          val otp = OTPGene.genOtp("", UserCache.get(token_id).get.get("TOKEN"))
          log info s"otp is ->$otp"
          val m = new java.util.HashMap[String, String]()
          m.put("OTP", otp)
          NotifyCenter.sendSMS("Map phone", m, null)

          JObject(
            JField("status", JInt(200)),
            JField("message", JString("发送成功")),
            JField("retry", JInt(30))
          )

        case "auth2fa" =>
          val code = getPara("code")
          if (code.isEmpty) {
            //TODO 监控下用于客户端体验质量的提升
            throw RESTfulException(400, "验证码格式错误")
          }
          val t = getPara("token")
          if (null == t || UserCache.get(t).isEmpty) {
            throw RESTfulException(403, "请重新登录")
          }

          if (!OTPGene.verifyOtp(UserCache.get(t).get.get("TOKEN"), code)) {
            throw RESTfulException(401, "认证失败")
          }

          setSessionAttr("uid", UserCache.get(t).get.uid.toString())
          JObject(
            JField("status", JInt(200)),
            JField("message", JString("认证成功"))
          )
      }
    } catch {
      case RESTfulException(code, message, more) =>
        var res = List(
          JField("status", JInt(code)),
          JField("message", JString(message)))
        more.foreach(kv => res ++= List(JField(kv._1, JString(kv._2))))
        JObject(res)
    }
  }

  def getLocaleTime(s: String): LocalDateTime = LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME)
}
