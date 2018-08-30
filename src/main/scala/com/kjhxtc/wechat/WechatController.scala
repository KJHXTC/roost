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

package com.kjhxtc.wechat

import com.jfinal.core.Controller
import com.kjhxtc.mwemxa.Logger

import scala.util.matching.Regex

/**
  * 为微信浏览器访问提供视图支持
  */
class WechatController extends Controller with Logger {
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
