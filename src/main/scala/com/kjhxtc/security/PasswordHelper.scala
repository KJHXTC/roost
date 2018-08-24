package com.kjhxtc.security

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.security._
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.util

import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Cipher, Mac, SecretKey}
import org.bouncycastle.util.encoders.Base64

import scala.collection.mutable
import scala.util.matching.Regex

object PasswordHelper {

  import scala.reflect.io.File

  val testKey = new SecretKeySpec("XXSAWEKSDJIDFNSK".getBytes, "AES/CFB8/NoPadding")
  var testPk: PublicKey = _
  val testPvk: PrivateKey = {
    val f = File("test.rsavk")
    if (f.exists) {
      //反序列化  也许可以用KeyStore

      val p = new ObjectInputStream(File("test.rsapk").inputStream())
      testPk = p.readObject() match {
        case x: RSAPublicKey =>
          x
      }
      val o = new ObjectInputStream(f.inputStream())
      o.readObject() match {
        case x: RSAPrivateKey =>
          x
      }
    } else {
      val rsa = KeyPairGenerator.getInstance("RSA")
      rsa.initialize(2048)
      val kp = rsa.generateKeyPair()
      val pvk = kp.getPrivate
      testPk = kp.getPublic
      val o = new ObjectOutputStream(File("test.rsavk").createFile(false).outputStream())
      o.writeObject(pvk)
      o.close()
      val p = new ObjectOutputStream(File("test.rsapk").createFile(false).outputStream())
      p.writeObject(testPk)
      p.close()
      pvk
    }
  }
  private[this] val providers: mutable.HashSet[PasswordHelper] = mutable.HashSet(new DefaultSoftSecureImpl())

  def add(c: PasswordHelper): Unit = {
    if (null == c || null == c.getName) throw new IllegalArgumentException("PasswordHelper should be nonEmpty")
    if (providers.exists(p => p.getName == c.getName)) throw new IllegalStateException("Provider already exists")
    providers.add(c)
  }

  def apply(provider: String): PasswordHelper = {
    providers.foreach(p =>
      if (p.getName == provider) return p
    )
    throw new IllegalStateException("No Such Provider: " + provider)
  }

  def apply(): PasswordHelper = {
    providers.headOption.getOrElse(throw new IllegalStateException("No Any Provider"))
  }

}

/**
  * 实现密码安全的几个要素
  * 1. 单向对称加密 即 仅提供加密,不提供解密 这样可以防止服务器被黑后,被黑客利用访问权限进行反向解码
  * 2. 验密操作在硬件密码机内执行,而不是通过外部解码对比 因此推荐硬加密；因为软加密意味着密码仍在服务器存储,黑客有获取密钥的可能
  */
trait PasswordHelper {

  /**
    * 标识
    *
    * @return
    */
  def getName: String

  /**
    * 客户端采用 公钥加密用户密码 的方式送入后台,然后后台使用对称算法保护客户密码存入数据库
    * 建议HTTPS,否则HTTP即使是公钥方式存在中间人劫持的可能(HTTP代理服务器), 所以 推荐用于控件方式的认证时采用
    *
    * @param key       前端公钥
    * @param secureKey 后台保护密钥
    * @param data      前端公钥加密的结果
    * @return 固定的自身可识别的密码格式,用于验证密码时识别
    */
  @throws(classOf[SecurityException])
  def makePassword(key: Key, secureKey: SecretKey, data: Array[Byte]): String

  /**
    * 客户端采用普通密码生成器方式
    *
    * @param secureKey 后台保护密钥
    * @param data      客户密码
    * @return 固定的自身可识别的密码格式,用于验证密码时识别
    */
  @throws(classOf[SecurityException])
  def makePassword(secureKey: SecretKey, data: Array[Byte]): String

  /**
    * 验证客户端密码的登录签名
    *
    * @param secureKey 后台保护密钥
    * @param signature 前端登录签名值
    * @param token     签名种子
    * @param passInDb  数据库存储的密码
    * @return
    */
  @throws(classOf[SecurityException])
  def verifyPassword(signature: Array[Byte], token: Array[Byte], secureKey: SecretKey, passInDb: String): Boolean
}

final case class PasswordNotSupportException(msg: String) extends SecurityException(msg)

/**
  * 使用 Base64 作为字符串编码方案
  * 使用 HMacSHA256 作为验证密码签名方案
  */
final class DefaultSoftSecureImpl extends PasswordHelper {
  val PasswordFormatInDB: String = "SOFT$V1$AES$%s"
  val PasswordFormatInDB_REG: Regex = "^SOFT\\$V1\\$AES\\$(%s)$".r

  private def encode(array: Array[Byte]): String = {
    Base64.toBase64String(array)
  }

  private def decode(data: String): Array[Byte] = {
    Base64.decode(data)
  }

  override def getName: String = "DefaultSoftSecureOfKEDYY"

  override def makePassword(key: Key, secureKey: SecretKey, data: Array[Byte]): String = {
    key match {
      case pvk: RSAPrivateKey =>
        val rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        rsa.init(Cipher.DECRYPT_MODE, pvk)
        val md = MessageDigest.getInstance("SHA-256")
        if (md.getDigestLength != data.length) {
          throw new IllegalArgumentException("Client Password Illegal")
        }
        val aes = Cipher.getInstance("AES/CFB8/NoPadding")
        aes.init(Cipher.ENCRYPT_MODE, secureKey)
        val xxx = aes.doFinal(data)
        PasswordFormatInDB.format(encode(xxx))
    }
  }

  override def makePassword(secureKey: SecretKey, data: Array[Byte]): String = {
    val aes = Cipher.getInstance("AES/CFB8/NoPadding")
    aes.init(Cipher.ENCRYPT_MODE, secureKey)
    val xxx = aes.doFinal(data)
    PasswordFormatInDB.format(encode(xxx))
  }

  override def verifyPassword(signature: Array[Byte], token: Array[Byte], secureKey: SecretKey, passwordInDb: String): Boolean = {
    val cipher = passwordInDb match {
      case PasswordFormatInDB_REG(value) => value
      case _ => throw PasswordNotSupportException("Slang is not my dish")
    }
    val aes = Cipher.getInstance("AES/CFB8/NoPadding")
    aes.init(Cipher.DECRYPT_MODE, secureKey)
    val key = aes.doFinal(decode(cipher))
    val m = Mac.getInstance("HMacSHA256")
    m.init(new SecretKeySpec(key, "HMacSHA256"))
    val y = m.doFinal(token)
    util.Arrays.equals(y, signature)
  }
}