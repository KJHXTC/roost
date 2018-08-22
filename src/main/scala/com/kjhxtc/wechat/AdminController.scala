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

import com.jfinal.core.ActionKey
import com.jfinal.weixin.sdk.api._
import com.jfinal.weixin.sdk.cache.DefaultAccessTokenCache
import com.jfinal.weixin.sdk.jfinal.ApiController
import com.kjhxtc.mwemxa.Logger

class AdminController extends ApiController with Logger {

  def index(): Unit = {
    renderText("main page")
  }

  @ActionKey("/weixin/admin")
  def admin(): Unit = {
    ApiConfigKit.setAccessTokenCache(new DefaultAccessTokenCache())

    val follows = UserApi.getFollows
    if (follows.getErrorCode != 0) {
      log error "Failed get user infos::" + follows.getErrorMsg
    }
    val sb = new StringBuilder()
    follows.getAttrs.forEach((a, b) =>
      sb.append(s"$a => ${b.toString}\n")
    )
    renderText(sb.toString())
  }

  @ActionKey("/weixin/connect")
  def connect(): Unit = {
    render("connect.html")
  }

  @ActionKey("/weixin/bind")
  def bind(): Unit = {
    if (getPara("status") == "success") {
      render("success.html")
    }
    else {
      render("error.html")
    }
  }
}
