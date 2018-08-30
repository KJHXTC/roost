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

package com.kjhxtc.mwemxa

import com.jfinal.template.Template

sealed abstract class SupportType(name: String)

case class EmailMsg(address: String) extends SupportType("EMAIL")

case class SMSMsg(phoneNumber: String) extends SupportType("MOBILE")

case class WEIXIN(openid: String) extends SupportType("WECHAT")

object NotifyCenter {

  def sendSMS(mobile: String, content: java.util.Map[String, String], template: Template) {
    val message = template.renderToString(content)

    println(s"$mobile ---> $message")
    true
  }

  def send(channel: SupportType, value: String): Boolean = {
    channel match {
      case EmailMsg(address) =>
        println("Send MAIL ... " + address + " Success")
        true
      case SMSMsg(phoneNumber) =>
        // 成本较高 每条至少 0.02RMB
        println("Send MAIL ... " + phoneNumber + " Success")
        true
      case WEIXIN(openid) =>

        true
    }
  }

  def sendEmail(mobile: String, content: java.util.Map[String, String], template: Template) {
    val message = template.renderToString(content)

    println(s"$mobile ---> $message")
    true
  }

  def pushApp(): Unit = {

  }

  def pushWechat() = {

  }
}
