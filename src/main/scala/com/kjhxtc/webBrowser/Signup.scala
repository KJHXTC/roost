package com.kjhxtc.webBrowser

import java.security.interfaces.RSAPublicKey
import java.security.{MessageDigest, SecureRandom}
import java.time.ZonedDateTime
import java.util.Base64

import com.jfinal.aop.Before
import com.jfinal.core.Controller
import com.jfinal.ext.interceptor.Restful
import com.kjhxtc.mwemxa.Model.User
import com.kjhxtc.mwemxa.{ISHelper, Logger, snowflake}
import com.kjhxtc.security.{OTPGene, PasswordHelper}
import org.bouncycastle.asn1.{ASN1EncodableVector, ASN1Integer, DERSequence}

@Before(Array {
  classOf[Restful]
})
class Signup extends Controller with Logger with ISHelper {
  /**
    * 当用户进行注册时,先在客户端产生随机ID
    */
  def index(): Unit = {

    val decoder = Base64.getDecoder
    val encoder = Base64.getEncoder
    val dist = MessageDigest.getInstance("SHA-1")
    val time = ZonedDateTime.now()
    val c = getCookie("CIRN", encoder.encodeToString(dist.digest(time.toString.getBytes)))
    val e = decoder.decode(c)
    val random = new SecureRandom(e)

    val buff = new Array[Byte](dist.getDigestLength)
    random.nextBytes(buff)
    val hash = dist.digest(buff)
    // 注册会话
    getSession(true).setAttribute("TOKEN", encoder.encodeToString(hash))

    setCookie("RID", encoder.encodeToString(buff), -1, "/signup", true)
    setAttr("CSRF_TOKEN", encoder.encodeToString(hash))
    val N = PasswordHelper.testPk.asInstanceOf[RSAPublicKey].getModulus
    val E = PasswordHelper.testPk.asInstanceOf[RSAPublicKey].getPublicExponent
    val vector = new ASN1EncodableVector()
    vector.add(new ASN1Integer(N))
    vector.add(new ASN1Integer(E))
    setAttr("RSA_PUBLIC_KEY", encoder.encodeToString(new DERSequence(vector).getEncoded("DER")))
    setAttr("SNS", "FaceBook")
    render("signup.html")
  }

  // POST
  def save(): Unit = {
    val csrf = getPara("CSRF_TOKEN")
    val dist = MessageDigest.getInstance("SHA-1")
    val cookie = getCookie("RID", "")

    val ua = getHeader("User-Agent")
    log info ua
    if (!snowflake.validUA(ua)) {
      renderError(403)
    }

    val token = getSession.getAttribute("TOKEN") match {
      case token: String =>
        token
      case _ => renderError(400)
    }

    val decoder = Base64.getDecoder
    val encoder = Base64.getEncoder

    val hash = dist.digest(decoder.decode(cookie))
    if (!token.equals(csrf) || !csrf.equals(encoder.encodeToString(hash))) {
      log error "CSRF ERROR"
      renderError(400)
    }

    val userEmail = getPara("signup_email") match {
      case REG_EMAIL(email) => email
      case _ => renderError(400, "Email Illegal")
    }

    val userName = getPara("signup_username") match {
      case REG_LOGIN(uuid) => uuid
      case _ => renderError(400, "UserName Illegal")
    }
    val db = new User()
    val exists = db.findByLogin(userName.toString)
    if (exists.nonEmpty) {
      index()
      return
    }

    //    使用公钥加密后,密码强度只能前端校验了
    //    val userPassword = try {
    //      checkPasswordPolice(getPara("signup_password"))
    //    } catch {
    //      case _: Throwable =>
    //        renderError(400)
    //        ""
    //    }
    //    val mess = MessageDigest.getInstance("SHA-256")
    //    val k = SecretKeyFactory.getInstance("PBKDF2withHMACSHA256")
    //    val salt = new Array[Byte](16)
    //    val random = new SecureRandom()
    //    random.nextBytes(salt)
    //    val key = k.generateSecret(new PBEKeySpec(userPassword.toCharArray, salt, 2000, mess.getDigestLength * 8)).getEncoded
    //    val pass = Base64.getEncoder.encodeToString(key)
    //    val ltoken = Base64.getEncoder.encodeToString(salt)
    val cipher = Base64.getDecoder.decode(getPara("signup_password"))
    val pass = PasswordHelper().makePassword(PasswordHelper.testPvk, PasswordHelper.testKey, cipher)
    db.set("ID", snowflake.getId(ua))
      .set("LOGIN", userName)
      .set("ROLE", "admin")
      .set("PASSWORD", pass)
      .set("TOKEN", OTPGene.randomSecure())
    db.save()
    redirect("/signin")
  }
}
