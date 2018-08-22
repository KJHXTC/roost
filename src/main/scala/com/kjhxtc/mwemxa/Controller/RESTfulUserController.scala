package com.kjhxtc.mwemxa.Controller

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.kjhxtc.mwemxa.Model.User
import com.kjhxtc.mwemxa.{ISHelper, Logger, NotifyCenter}
import com.kjhxtc.security.OTPGene
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonAST._

sealed case class RESTfulException(code: Int, message: String, map: Map[String, String] = Map()) extends Exception(message)


class RESTfulUserController extends JsonServiceCtrl with Logger with ISHelper {

  override def data: JsonAST.JValue = {
    if (Option(getCookie("CIRN")).isEmpty) {
      log debug "New Client ..."
      setCookie("CIRN", randomData(32), Int.MaxValue, true)
    }
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

          val clt = Option[String](getPara("salt"))
          val svc = randomData(24)
          val token = hash(clt.getOrElse(throw RESTfulException(400, "参数错误")), svc)
          // 缓存账号信息
          UserCache.set(token, login.getOrElse(throw RESTfulException(404, "无效的用户名")))
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
          val token_id = getPara("token")
          if (null == token_id || UserCache.get(token_id).isEmpty) {
            throw RESTfulException(403, "请重新登录")
          }
          val u = UserCache.get(token_id).get
          var c: List[JString] = Nil
          if (!u.verification(signature, token_id)) {
            throw RESTfulException(401, "认证失败,请检查登录名和密码")
          } else {
            if (u.hasEmail) c ++= List(JString("EMAIL"))
            if (u.hasPhone) c ++= List(JString("SMS"))
            if (u.has2FA) c ++= List(JString("Authenticator"))
          }

          if (c == Nil) {
            JObject(
              JField("status", JInt(200)),
              JField("enable2fa", JBool(false)),
              JField("title", JString("绑定成功")),
              JField("message", JString("您的账号已成功与微信绑定"))
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
          NotifyCenter.sendSMS("Map phone", m, smsTemplate)

          JObject(
            JField("status", JInt(200)),
            JField("values", JString("发送成功")),
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

          JObject(
            JField("status", JInt(200)),
            JField("title", JString("绑定成功")),
            JField("message", JString("您的账号已成功与微信绑定"))
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
