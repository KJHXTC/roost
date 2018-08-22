package com.kjhxtc.mwemxa

import com.jfinal.config._
import com.jfinal.core.ActionHandler
import com.jfinal.ext.handler.RoutesHandler
import com.jfinal.handler.Handler
import com.jfinal.kit.PropKit
import com.jfinal.log.Log
import com.jfinal.plugin.activerecord.{ActiveRecordPlugin, DbKit}
import com.jfinal.plugin.c3p0.C3p0Plugin
import com.jfinal.plugin.hikaricp.HikariCpPlugin
import com.jfinal.render.ViewType
import com.jfinal.template.Engine
import com.jfinal.weixin.sdk.api.{ApiConfig, ApiConfigKit}
import com.kjhxtc.mwemxa.Controller._
import com.kjhxtc.mwemxa.Model._
import com.kjhxtc.webBrowser.{Signin, Signup}
import com.kjhxtc.wechat.{AdminController, _}
import com.kjhxtc.weixin.WeixinMsgController
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class SysConfig extends JFinalConfig with Logger {
  log warn "loading system config"

  override def configConstant(me: Constants): Unit = {
    me.setDevMode(true)
    me.setEncoding("UTF-8")
    me.setI18nDefaultLocale("zh_CN")
    me.setViewType(ViewType.FREE_MARKER)
  }

  override def configRoute(me: Routes): Unit = {
    me.setBaseViewPath("/template")
    me.add("/signup", classOf[Signup])
    me.add("/signin", classOf[Signin])
    me.add("/login", classOf[Signin])
    me.add("/users", classOf[UserController])
    me.add("/wechat/", classOf[WeixinMsgController])
    me.add("/weixin/", classOf[AdminController])

    me.add("/accounts/wx/bind", classOf[RESTfulUserController])
    registerHandlerWX(me)
  }

  def registerHandlerWX(me: Routes): Unit = {
    val prop = loadPropertyFile("wx.properties")

    val ac = new ApiConfig()
    ac.setToken(prop.getProperty("token"))
    ac.setAppId(prop.getProperty("appId"))
    ac.setAppSecret(prop.getProperty("appSecret"))
    ApiConfigKit.putApiConfig(ac)

    me.add(prop.getProperty("uri"), classOf[WeixinMsgController])
  }

  override def configEngine(me: Engine): Unit = {
  }

  override def configPlugin(me: Plugins): Unit = {
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
    active.addMapping("wechat_user", classOf[WechatUser])

    me.add(plugin)
    me.add(active)
  }

  override def configInterceptor(me: Interceptors): Unit = {

  }

  override def configHandler(me: Handlers): Unit = {

  }
}
