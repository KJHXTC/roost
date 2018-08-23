package com.kjhxtc.mwemxa.Controller

import com.jfinal.aop.Before
import com.jfinal.core.Controller
import com.kjhxtc.mwemxa.Logger
import net.liftweb.json
import net.liftweb.json.JsonAST._


/**
  * 服务通用的JSON View 渲染
  * JFinal 默认的需要写视图
  */
abstract class JsonServiceCtrl extends Controller with Logger {
  def data: JValue

  def index(): Unit = {
    val restful = try {
      data
    } catch {
      case th: Throwable =>
        // Stat.moniter(th, Session)
        throw th
    }
    renderJson(json.compactRender(restful))
  }
}
