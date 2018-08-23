package com.kjhxtc.wechat

import java.util.Date

import com.jfinal.plugin.activerecord.Db
import com.jfinal.weixin.sdk.api.{ApiConfig, ApiConfigKit}
import com.jfinal.weixin.sdk.cache.{DefaultAccessTokenCache, IAccessTokenCache}
import com.kjhxtc.mwemxa.Logger
import com.kjhxtc.mwemxa.Model.WeChatGH
import net.liftweb.json
import net.liftweb.json.JsonAST.{JField, JObject}
import net.liftweb.json.{JInt, JString}


class Aimp(dao: WeChatGH) extends IAccessTokenCache with Logger {
  private[this] var cache = new DefaultAccessTokenCache()

  override def get(key: String): String = synchronized {
    if (cache.get(key) != null) return cache.get(key)
    log debug "key->" + key
    var x = ""
    var y = new Date()
    dao.find("SELECT * FROM WECHAT_GH WHERE APPID=?", key).forEach(row => {
      x = row.getStr("accessToken")
      y = row.getDate("expiredTime")
    })
    if (x != null && y != null && x.nonEmpty && y.after(new Date())) {
      val jvs = JObject(
        JField("access_token", JString(x)),
        JField("expiredTime", JInt(y.getTime / 1000))
      )
      cache.set(key, json.prettyRender(jvs))
    }
    cache.get(key)
  }

  override def set(key: String, jsonValue: String): Unit = synchronized {
    var token: String = ""
    var time: Long = 0
    json.parse(jsonValue).mapField(f => {
      f.name match {
        case "access_token" =>
          token = f.value.asInstanceOf[JString].values
        case "expiredTime" =>
          time = f.value.asInstanceOf[JInt].values.toLong
      }
      f
    })

    log debug "cache token " + token + "  " + time
    if (token.nonEmpty && time > 0) {
      Db.update("UPDATE WECHAT_GH set accessToken=? , expiredTime=? WHERE APPID=?", token, new Date(time), key)
    }

    cache.set(key, jsonValue)
  }

  override def remove(key: String): Unit = {
    log debug "do nothing"
    cache.remove(key)
  }
}

object WeChatConfig extends Logger {
  def init(): Unit = {
    log debug "init WeChat Server Config"
    val wx = new WeChatGH().dao()
    ApiConfigKit.setAccessTokenCache(new Aimp(wx))
    wx.find("SELECT * FROM wechat_gh LIMIT 5").forEach {
      iter => {
        val ac = new ApiConfig()
        ac.setToken(iter.getStr("token"))
        ac.setAppId(iter.getStr("appID"))
        ac.setAppSecret(iter.getStr("appSecret"))
        ApiConfigKit.putApiConfig(ac)
      }
    }
  }
}
