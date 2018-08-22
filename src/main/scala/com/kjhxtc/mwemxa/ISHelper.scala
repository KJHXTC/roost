package com.kjhxtc.mwemxa

import java.security.{MessageDigest, SecureRandom}
import java.util.Base64

import scala.util.matching.Regex


trait ISHelper {
  /**
    * 列出你所需的即可 没有必要全部写入
    *
    * @see
    * `<a href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1</a>`
    * `<a href="https://en.wikipedia.org/wiki/Country_code_top-level_domain"> ccTLDs </a>`
    */
  lazy val COUNTRY_CODE = Set("US", "CN", "JP", "HK", "TW", "KR", "AU")

  /**
    * 邮箱地址应该符合RFC规范
    *
    * @see
    * `<a href="http://tools.ietf.org/html/rfc3696">RFC 3696</a>`
    * `<a href="https://segmentfault.com/a/1190000000631567">引用的正则式来源</a>`
    */
  lazy val REG_EMAIL: Regex = "^([A-Za-z0-9!#$%&'+/=?^_`{|}~-]+(?:.[A-Za-z0-9!#$%&'+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9]+(?:-[A-Za-z0-9]+)?.)+[A-Za-z0-9]+(?:-[A-Za-z0-9]+)?)$".r

  /**
    * 号码应符合 +N XXX-YYY-ZZZZ
    * +1 204-400-3322 US
    * +86 [0]10-8888-6666 CN TEL
    * +86 [0]755-6666-6666 CN TEL
    * +86 138-138-00138 CN MOB
    * +852 123-456-7890
    * 这里正则提取两个值 国家码 和国内号码
    */
  lazy val REG_Phone: Regex = "\\+([\\d]{1,3}) ([\\d]{7,13})".r
  /**
    * 密码策略:
    * <p>至少 8位(包含数字 小写字母 大写字母 特殊字符 任意至少 3种) 不能包含空白字符也就是空白字符需要被剔除</p>
    * <ul>
    * <li>数字 0-9</li>
    * <li>小写字母 a-z</li>
    * <li>大写字母 A-Z</li>
    * <li>特殊字符 . _ ! # @ $ % * + - ~</li>
    * </ul>
    */
  lazy val REG_PASSWORD: Regex = s"^([0-9a-zA-Z$PERMIT_CHAR]{8,30})$$".r
  /**
    * 用户唯一标记ID,应该由系统自动分配
    * <p>推荐参照 推特的<a href="https://github.com/twitter/snowflake)">snowflake</a>算法</p>
    *
    * 类似于中国的身份证号分配规则
    * 一般来讲在生成后不允许再次修改
    */
  lazy val REG_G_UUID: Regex = "".r
  /**
    * 用户登录名:<b>用户自己设定的唯一登录标识</b>
    * <p>规则一般根据网站自身的需求设定<p>
    * <p>具有全局唯一性, 可以一段时间内修改 也可以不允许修改</p>
    * 这里默认只允许(不区分大小写) 字母开头的 字母数字 和下划线的 登录名
    * <p>如果希望自定义可重写</p>
    *
    * @example
    * 例如 百度 允许中文的用户名(登录名 且不允许修改)
    *
    */
  lazy val REG_LOGIN: Regex = "^((?:[a-zA-Z][a-zA-Z0-9_]{5,19})|(?:[\\u4e00-\\u9fa5]{2,10}))$".r

  def checkPasswordPolice(password: String, asLeast: Int = 3): String = {
    val p = password match {
      case REG_PASSWORD(word) => word
    }
    var checked = 0
    if (p.exists(c => c.isDigit)) {
      checked += 1
    }
    if (p.exists(c => c.isLower)) {
      checked += 1
    }
    if (p.exists(c => c.isUpper)) {
      checked += 1
    }
    if (p.exists(c => PERMIT_CHAR.contains(c))) {
      checked += 1
    }
    println(checked)
    if (checked < asLeast) throw new IllegalArgumentException("Password Illegal")
    p
  }

  def PERMIT_CHAR: String = "_\\.\\!\\$\\#\\%\\*\\+\\-\\~"

  def formatPhoneNumber(number: String): (String, String) = {
    val phone = number.trim.replace("-", "")
    phone match {
      case REG_Phone(cc, cn) => (cc, cn)
    }
  }

  def randomData(len: Int): String = {
    val b = new Array[Byte](len)
    val rand = new SecureRandom()
    rand.nextBytes(b)
    Base64.getEncoder.encodeToString(b)
  }

  def hash(c: String, s: String, method: String = "SHA-256"): String = {
    val mess = MessageDigest.getInstance(method)
    mess.update(Base64.getDecoder.decode(c))
    mess.update(Base64.getDecoder.decode(s))
    Base64.getEncoder.encodeToString(mess.digest())
  }
}

object test extends App with ISHelper {

  "abcef2342W@mail.com" match {
    case REG_EMAIL(e) => println(e)
  }

  "ADSF" match {
    case REG_LOGIN(e) => println(e)
  }
  "A3a1231231231" match {
    case REG_PASSWORD(pa) => println(pa)
  }


  println("done")

}