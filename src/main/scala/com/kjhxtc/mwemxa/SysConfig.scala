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

import com.jfinal.config._
import com.jfinal.plugin.activerecord.ActiveRecordPlugin
import com.jfinal.plugin.c3p0.C3p0Plugin
import com.jfinal.render.ViewType
import com.jfinal.template.Engine
import com.jfinal.weixin.sdk.api.ApiConfigKit
import com.kjhxtc.mwemxa.Controller._
import com.kjhxtc.mwemxa.Model._
import com.kjhxtc.webBrowser.{Signin, Signup}
import com.kjhxtc.wechat.{AdminController, WechatController}
import com.kjhxtc.weixin.{WeChatConfig, WeixinController}

class SysConfig extends JFinalConfig with Logger {
  log warn "loading system config"

  override def configConstant(me: Constants): Unit = {
    log info "加载常规配置 ..."
    me.setDevMode(true)
    ApiConfigKit.setDevMode(true)
    me.setEncoding("UTF-8")
    me.setI18nDefaultLocale("zh_CN")
    me.setViewType(ViewType.FREE_MARKER)
  }

  override def configRoute(me: Routes): Unit = {
    log info "配置路由 ..."
    me.setBaseViewPath("/template")
    me.add("/signup", classOf[Signup])
    me.add("/signin", classOf[Signin])
    me.add("/login", classOf[Signin])
    me.add("/users", classOf[UserController])
    me.add("/wechat/", classOf[WechatController])
    me.add("/weixin/", classOf[AdminController])
    // 帐号中心进行绑定的
    me.add("/accounts/bind", classOf[RESTfulUserController])
    // 微信公众号应答服务器地址
    me.add("/wx", classOf[WeixinController])
  }

  override def configEngine(me: Engine): Unit = {
    log info "加载引擎 .."
  }

  override def configPlugin(me: Plugins): Unit = {
    log info "加载数据库 .."
    val p = loadPropertyFile("db.properties")

    val plugin = new C3p0Plugin(
      p.getProperty("jdbcUrl"),
      p.getProperty("user"),
      p.getProperty("password")
    )

    val active = new ActiveRecordPlugin(plugin)
    active.addMapping("role", classOf[Role])
    active.addMapping("user", classOf[User])
    active.addMapping("tb_profile", classOf[Profile])
    active.addMapping("wx_user", classOf[WechatUser])
    active.addMapping("wx_gh", classOf[WeChatGH])

    me.add(plugin)
    me.add(active)
  }

  override def configInterceptor(me: Interceptors): Unit = {
    log info "加载拦截器 .."
  }

  override def configHandler(me: Handlers): Unit = {
    log info "加载处理器 .."

  }

  override def afterJFinalStart(): Unit = {
    Jobs.scheduler.start()
    WeChatConfig.init()
  }

  override def beforeJFinalStop(): Unit = {
    Jobs.scheduler.shutdown()
  }
}
