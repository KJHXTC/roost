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

package com.kjhxtc.security

import java.security._
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.spec.AlgorithmParameterSpec
import java.util

import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Cipher, Mac, SecretKey}
import org.bouncycastle.util.encoders.Base64

import scala.collection.mutable
import scala.util.matching.Regex

object AuthenticateHelper {

  val testKey = new SecretKeySpec("XXSAWEKSDJIDFNSK".getBytes, "AES")
  private[this] val providers: mutable.HashSet[AuthenticateHelper] = mutable.HashSet(new DefaultSoftSecureImpl())

  def add(c: AuthenticateHelper): Unit = {
    if (null == c || null == c.getName) throw new IllegalArgumentException("PasswordHelper should be nonEmpty")
    if (providers.exists(p => p.getName == c.getName)) throw new IllegalStateException("Provider already exists")
    providers.add(c)
  }

  def apply(provider: String): AuthenticateHelper = {
    providers.foreach(p =>
      if (p.getName == provider) return p
    )
    throw new IllegalStateException("No Such Provider: " + provider)
  }

  def apply(): AuthenticateHelper = {
    providers.headOption.getOrElse(throw new IllegalStateException("No Any Provider"))
  }
}

/**
  * 实现密码安全的几个要素
  * 1. 单向对称加密 即 仅提供加密,不提供解密 这样可以防止服务器被黑后,被黑客利用访问权限进行反向解码
  * 2. 验密操作在硬件密码机内执行,而不是通过外部解码对比 因此推荐硬加密；因为软加密意味着密码仍在服务器存储,黑客有获取密钥的可能
  */
trait AuthenticateHelper {

  /**
    * 标识
    *
    * @return
    */
  def getName: String

  /**
    * 客户端采用普通密码生成器方式
    *
    * @param secureKey 后台保户客户密码的密钥
    * @param data      客户的明文密码(ASCII键盘可见字符)
    * @return 固定的自身可识别的密码格式,用于验证密码时识别
    */
  @throws(classOf[SecurityException])
  def makePassword(secureKey: SecretKey, data: Array[Byte]): String

  /**
    * 客户端采用 (公钥算法|对称算法)加密用户密码的方式将密码送入后台,然后后台使用对称算法保护客户密码存入数据库
    * 建议HTTPS,否则HTTP即使是公钥方式也存在中间人劫持的可能(例如恶意的HTTP代理服务器)
    * 安全级别较高时推荐采用安全控件方式的认证 也可以采用UKey等硬件
    *
    * @param key          前端加密密钥
    * @param keyParameter 前端加密密钥参数
    * @param data         前端公钥加密的结果
    * @param secureKey    后台保户客户密码的密钥
    * @return 固定的自身可识别的密码格式,用于验证密码时识别
    */
  @throws(classOf[SecurityException])
  def makePassword(key: Key, keyParameter: Option[AlgorithmParameterSpec] = None, data: Array[Byte], secureKey: SecretKey): String

  /**
    * 验证客户端密码的登录签名
    *
    * @param signature 前端登录签名值
    * @param data      签名种子数据
    * @param secureKey 后台保户客户密码的密钥
    * @param passInDb  数据库存储的密码
    * @throws SecurityException 验证失败/异常时
    */
  @throws(classOf[SecurityException])
  def verifyPassword(signature: Array[Byte], data: Array[Byte], secureKey: SecretKey, passInDb: String): Unit

  /**
    * 计算签名数据
    *
    * @param key  签名密钥:对称算法/哈希算法/非对称算法
    * @param data 签名数据
    * @return 签名值(编码策略编码过)
    */
  @throws(classOf[SecurityException])
  def makeSignature(data: Array[Byte], key: Key): String

  /**
    * 验证签名数据
    *
    * @param signature 待验证的签名值
    * @param token     签名数据
    * @param key       签名密钥:对称算法/哈希算法/非对称算法
    * @throws SecurityException 验证失败/异常时
    */
  @throws(classOf[SecurityException])
  def verifySignature(signature: Array[Byte], token: Array[Byte], key: Key): Unit
}

final case class PasswordNotSupportException(msg: String) extends SecurityException(msg)

final case class PasswordMissMatchException(msg: String) extends SecurityException(msg)

final case class SignatureMissMatchException(msg: String) extends SecurityException(msg)

/**
  * 使用 Base64 作为字符串编码方案
  * 使用 HMacSHA256 作为验证密码签名方案
  */
final class DefaultSoftSecureImpl extends AuthenticateHelper {
  val PasswordFormatInDB: String = "SOFT$V1$AES$%s"
  val PasswordFormatInDB_REG: Regex = "^SOFT\\$V1\\$AES\\$([0-9a-zA-Z/\\+=]{6,127})$".r

  private def encode(array: Array[Byte]): String = {
    Base64.toBase64String(array)
  }

  private def decode(data: String): Array[Byte] = {
    Base64.decode(data)
  }

  override def getName: String = "Default"

  override def makePassword(key: Key, keyParameter: Option[AlgorithmParameterSpec], data: Array[Byte], secureKey: SecretKey): String = {
    key match {
      case sek: SecretKey =>
        val aes = Cipher.getInstance("AES/CFB/NoPadding")
        if (keyParameter.nonEmpty)
          aes.init(Cipher.DECRYPT_MODE, sek, keyParameter.get)
        else
          aes.init(Cipher.DECRYPT_MODE, sek)
        val password = aes.doFinal(data)
        makePassword(secureKey = secureKey, data = password)

      case pvk: RSAPrivateKey =>
        val rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        if (keyParameter.nonEmpty)
          rsa.init(Cipher.DECRYPT_MODE, pvk, keyParameter.get)
        else
          rsa.init(Cipher.DECRYPT_MODE, pvk)

        val password = rsa.doFinal(data)
        makePassword(secureKey, password)
    }
  }

  override def makePassword(secureKey: SecretKey, data: Array[Byte]): String = {
    try {
      val aes = Cipher.getInstance("AES/ECB/PKCS5Padding")
      aes.init(Cipher.ENCRYPT_MODE, secureKey)
      val cipher = aes.doFinal(data)
      PasswordFormatInDB.format(encode(cipher))
    } catch {
      case th: Throwable =>
        throw new SecurityException(th)
    }
  }


  override def verifyPassword(signature: Array[Byte], token: Array[Byte], secureKey: SecretKey, passwordInDb: String): Unit = {
    val cipher = passwordInDb match {
      case PasswordFormatInDB_REG(value) => value
      case _ => throw PasswordNotSupportException("Slang is not my dish")
    }
    val key = try {
      val aes = Cipher.getInstance("AES/ECB/PKCS5Padding")
      aes.init(Cipher.DECRYPT_MODE, secureKey)
      aes.doFinal(decode(cipher))
    } catch {
      case th: Throwable => throw new SecurityException(th)
    }
    val exceptSignature = try {
      val mac = Mac.getInstance("HMacSHA256")
      mac.init(new SecretKeySpec(key, "HMacSHA256"))
      mac.doFinal(token)
    } catch {
      case th: Throwable => throw new SecurityException(th)
    }
    if (!util.Arrays.equals(exceptSignature, signature)) {
      throw PasswordMissMatchException("Password Verify Failed")
    }
  }

  override def makeSignature(data: Array[Byte], key: Key): String = {
    key match {
      /**
        * 软实现 我们采用HMacSHA256作为标准
        * 当然 像银行一般采用对称CMac
        */
      case secureKey: SecretKey =>
        val mac = Mac.getInstance("HMacSHA256")
        mac.init(new SecretKeySpec(secureKey.getEncoded, "HMacSHA256"))
        val x = mac.doFinal(data)
        encode(x)

      /**
        * 当用户采用 RSA作为认证手段时
        */
      case _: RSAPublicKey =>
        //TODO
        ???
    }
  }

  override def verifySignature(signature: Array[Byte], token: Array[Byte], key: Key): Unit = {
    val exceptSignature = try {
      decode(makeSignature(token, key))
    } catch {
      case th: Throwable => throw new SecurityException(th)
    }
    if (!util.Arrays.equals(exceptSignature, signature)) {
      throw SignatureMissMatchException("Signature Verify Failed")
    }
  }
}