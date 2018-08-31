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

import com.google.common.base.Throwables
import com.jfinal.core.Controller
import com.kjhxtc.mwemxa.{ISHelper, Logger}


private[webBrowser] abstract class WebException(message: String) extends IllegalArgumentException(message)

private[webBrowser] case class BadRequestException(message: String) extends WebException(message) {
  val code: Int = 400
}

private[webBrowser] case class AuthenticateException(message: String) extends WebException(message) {
  val code: Int = 401
}

private[webBrowser] case class ForbiddenException(message: String) extends WebException(message) {
  val code: Int = 403
}

private[webBrowser] abstract class MainController extends Controller with Logger with ISHelper {


  def onGet(): Unit

  def onPost(): Unit

  def doAction(f: () => Unit): Unit = {
    try {
      f()
    } catch {
      case ex: BadRequestException =>
        log info "client error" + ex.message
        //TODO  自定义错误页面
        renderError(ex.code)
      case ex: ForbiddenException =>
        log info "client error" + ex.message
        //TODO  自定义错误页面
        renderError(ex.code)
      case ex: AuthenticateException =>
        log info "client error" + ex.message
        renderError(ex.code)
      case throwable: Throwable =>
        // TODO 监控非预期异常
        log error Throwables.getStackTraceAsString(throwable)
        renderError(500)
    }
  }

  def index(): Unit = {
    doAction(onGet)
  }

  def save(): Unit = {
    doAction(onPost)
  }
}
