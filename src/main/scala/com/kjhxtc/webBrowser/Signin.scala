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

package com.kjhxtc.webBrowser

import com.jfinal.aop.Before
import com.jfinal.core.Controller
import com.jfinal.ext.interceptor.Restful
import com.kjhxtc.mwemxa.Model.User
import com.kjhxtc.mwemxa.{ISHelper, Logger}

sealed abstract class LoginType

case object EmailLogin extends LoginType

case object PhoneLogin extends LoginType

case object NameLogin extends LoginType

@Before(Array {
  classOf[Restful]
})
class Signin extends Controller with Logger with ISHelper {
  //login get
  def index(): Unit = {
    getSession(true)
    val d = getCookie("BS_UUID", randomData(32))
    setCookie("BS_UUID", d, Int.MaxValue)

    setAttr("SNS", "FaceBook")
    render("signin.html")
  }

  //login post
  def save(): Unit = {
    log info getHeader("Refer")
    val logins: (LoginType, String) = getPara("signin_username") match {
      case REG_EMAIL(email) => (EmailLogin, email)
      case REG_LOGIN(name) => (NameLogin, name)
      case REG_Phone(cn, id) => (PhoneLogin, "+%s %s" format(cn, id))
      case _ =>
        log error "Login Type unknown"
        throw new Exception("Invalid login id")
    }
    val password = getPara("signin_password")

    val u = new User()
    val found = logins._1 match {
      case EmailLogin =>
        u.findByEmail(logins._2)
      case NameLogin =>
        u.findByLogin(logins._2)
    }
    if (found.isEmpty) {
      log error s"User [${logins._2}] Not Exists!"
      renderError(401)
      return
    }
    log info password

    renderText("Login Success")
  }
}
