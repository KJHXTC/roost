package com.kjhxtc.weixin

import com.jfinal.core.Controller
import com.kjhxtc.mwemxa.Logger

import scala.util.matching.Regex

/**
  * 为微信浏览器访问提供视图支持
  */
class WeixinController extends Controller with Logger {
  protected val REG_MicroMessenger: Regex =
    "^Mozilla(?:.+)MicroMessenger/([\\d]\\.[\\d]\\.[\\d]) NetType/(\\w{1,6}) Language/(\\w{5})".r

  def connect(): Unit = {
    // For Secure delay 1 second
    val header = getHeader("User-Agent")
    log info header
    header match {
      case REG_MicroMessenger(version, stat, lang) =>
        log info version
        log info stat
        log info lang
      case _ =>
        log error "请使用微信浏览器打开"
    }
    render("/signin/signin.html")
  }
}
