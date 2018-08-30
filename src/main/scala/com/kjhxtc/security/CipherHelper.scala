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

import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.{Key, Security}

import javax.crypto.spec.IvParameterSpec
import javax.crypto.{Cipher, SecretKey}
import org.bouncycastle.util.encoders.{Base64, Hex}

/**
  * 用于重要隐私数据加密,也请注意 此类数据字段不要设计为唯一属性
  */
trait CipherHelper extends Provider {
  /**
    * 加密重要数据(如证件号,银行卡号等)
    *
    * @param message UTF8文字数据
    * @param key     密钥对称算法|非对称算法
    * @param uid     用户号(数字字符)
    * @return 编码后的加密结果
    */
  def encrypt(message: String, key: Key, uid: String): String

  /**
    * 解密重要数据(如证件号,银行卡号等)
    *
    * @param data 密文数据
    * @param key  密钥对称算法|非对称算法
    * @param uid  用户号(数字字符)
    * @return UTF8文字数据
    */
  def decrypt(data: String, key: Key, uid: String): String

  /**
    * 采用的编码名称如 (Base64/Hex 等)
    */
  def encoding: String
}

object CipherHelper extends Template[CipherHelper] {
  add(new DefaultSoftCipherImpl())
}

/**
  * 默认实现样例,使用到 `BouncyCastleProvider`的加密库
  * 使用 Base64 作为字符串编码方案
  * 使用 AES/CFB/PKCS7Padding 作为数据加密方案
  */
final class DefaultSoftCipherImpl extends CipherHelper {
  // 使用BC软加密库实现
  Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())

  private def encode(array: Array[Byte]): String = {
    Base64.toBase64String(array)
  }

  private def decode(data: String): Array[Byte] = {
    Base64.decode(data)
  }

  private def uid2data(uid: String): Array[Byte] = {
    if (null == uid || uid.isEmpty || uid.length > 32) throw new IllegalArgumentException("UID is Empty or too long")
    uid.foreach(c => if (!c.isDigit) throw new IllegalArgumentException("UID Invalid"))
    val keyHex = uid.reverse.padTo(32, '0').reverse
    Hex.decode(keyHex)
  }

  override def getName: String = "Default"

  override def encrypt(message: String, key: Key, uid: String): String = {
    val data = message.getBytes("UTF-8")
    val result = try {
      key match {
        case sek: SecretKey =>
          val cipher = Cipher.getInstance("AES/CFB/PKCS7Padding", "BC")
          cipher.init(Cipher.ENCRYPT_MODE, sek, new IvParameterSpec(uid2data(uid)))
          cipher.doFinal(data)
        case pk: RSAPublicKey =>
          ???
      }
    } catch {
      case error: NotImplementedError =>
        throw error
      case throwable: Throwable =>
        throw throwable
    }
    encode(result)
  }

  override def decrypt(data: String, key: Key, uid: String): String = {
    val cipherData = decode(data)
    val result = try {
      key match {
        case sek: SecretKey =>
          val cipher = Cipher.getInstance("AES/CFB/PKCS7Padding", "BC")
          cipher.init(Cipher.DECRYPT_MODE, sek, new IvParameterSpec(uid2data(uid)))
          cipher.doFinal(cipherData)
        case vk: RSAPrivateKey =>
          ???
      }
    } catch {
      case throwable: Throwable =>
        throw throwable
    }
    new String(result, "UTF-8")
  }

  override def encoding: String = "Base64"
}