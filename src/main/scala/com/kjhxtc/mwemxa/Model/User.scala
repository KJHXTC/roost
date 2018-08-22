package com.kjhxtc.mwemxa.Model

import java.security.MessageDigest
import java.util.Base64

import com.jfinal.plugin.activerecord.Model
import com.kjhxtc.mwemxa.Logger
import com.kjhxtc.security.PasswordHelper
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class User extends Model[User] with Logger {
  /*
   *  用户信息 用于认证 谁是谁
   *  ID, G-UUID,
   *  ROLE [ADMIN, USER, DCP ....]
   *  Password       (Login) 静态口令,         认证权重 2
   *  KeyGenerate    (TOKEN) 动态口令,         认证权重 EMAIL-4 SMS-4 HARDWARE-5
   *  SSH-KEY(SSH公钥)       服务器登录         认证权重 3
   *  PGP-KEY(PGP公钥)       数据传输           认证权重 3-6 [视 文件加密权重]
   *  X509-CertKey(公钥证书)  身份认证|U-KEY登录  认证权重 3-7 [视 用户协议而定] 安全性由用户衡量 对于多数是小白用户的最好为3
   *  PKCS#8 KEY     (Key)   静态口令保护的私钥   数据存储 可能被更新
   *  SECURE-INFO-KEY(数字信封) 数字信封格式加密的用户明文密码-用于转换加密密钥 数据存储 可能被PKCS#8 关联更新
   *
   *  同等级权重的可替换同等级或低等级的认证
   *  认证权重达到 7 以及以上的,衡量为安全 可以任意 修改
   */

  var uid: BigInt = -1

  import scala.collection.JavaConverters._

  def findByLogin(login: String): Option[User] = {
    val sql = "SELECT * FROM user WHERE LOGIN = ?"
    val found = this.find(sql, login)
    if (found.size() > 0) {
      val u = found.get(0)
      u.uid = found.get(0).getBigInteger("ID")
      Some(u)
    }
    else None
  }

  def findByEmail(login: String): Option[User] = {
    val sql = "SELECT * FROM user WHERE ID = (SELECT UID FROM tb_profile WHERE EMAIL = ?)"
    val found = this.find(sql, login)
    if (found.size() > 0) {
      val u = found.get(0)
      u.uid = found.get(0).getBigInteger("ID")
      Some(u)
    }
    else None
  }

  def findByPhone(cc: String, phone: String): Option[User] = {
    val sql = "SELECT * FROM user WHERE ID = (SELECT UID FROM tb_profile WHERE MOBILE = ?)"
    val found = this.find(sql, phone)
    if (found.size() > 0) {
      val u = found.get(0)
      u.uid = found.get(0).getBigInteger("ID")
      Some(u)
    }
    else None
  }

  /**
    * 验证用户密码
    *
    * @param signature Base64编码的值
    * @return
    */
  def verification(signature: String, token: String, throwable: Option[Throwable] = None): Boolean = {
    val passwordOfDB = Option(getStr("PASSWORD"))
    if (passwordOfDB.isEmpty) {
      // 禁止空密码认证
      throwable.foreach(th => throw th)
      return false
    }

    // TODO 经硬件或其他安全设备进行签名值的计算
    // 取出的是经过系统加密的密码,传给硬件加密机进行鉴别
    PasswordHelper().verifyPassword(Base64.getDecoder.decode(signature), Base64.getDecoder.decode(token), PasswordHelper.testKey, passwordOfDB.get)
  }

  def hasEmail: Boolean = {
    val sql = "SELECT * from tb_email WHERE UID = ?"
    if (uid < 0) {
      throw new IllegalStateException("execute find first")
    }
    val found = find(sql, uid)
    if (found.size() > 0) {
      true
    } else {
      false
    }
  }

  def hasPhone: Boolean = {
    val sql = "SELECT * from tb_profile WHERE UID = ?"
    if (uid < 0) {
      throw new IllegalStateException("execute find first")
    }
    val found = find(sql, uid)
    if (found.size() > 0) {
      Option(found.get(0).getBigInteger("phone")).isDefined
    } else {
      false
    }
  }

  /**
    * 用户是否启用了 两步认证软件
    *
    * @return
    */
  def has2FA: Boolean = {
    //TODO
    Option(getStr("TOKEN")).nonEmpty
  }
}
