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

package com.kjhxtc.weixin

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
  private[this] val cache = new DefaultAccessTokenCache()

  override def get(key: String): String = synchronized {
    if (cache.get(key) != null) {
      json.parse(cache.get(key)).mapField(f => if (f.name == "expiredTime") {
        if (f.value.asInstanceOf[JInt].num.toLong - 60 > System.currentTimeMillis() / 1000) {
          return cache.get(key)
        }
        f
      } else f)
    }
    log debug "key->" + key
    var x = ""
    var y = new Date()
    dao.find("SELECT * FROM WX_GH WHERE appID=?", key).forEach(row => {
      x = row.getStr("accessToken")
      y = row.getDate("expiredTime")
    })
    if (x != null && y != null && x.nonEmpty && y.after(new Date())) {
      val jvs = JObject(
        JField("access_token", JString(x)),
        JField("expiredTime", JInt(y.getTime / 1000))
      )
      cache.set(key, json.compactRender(jvs))
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
      Db.update("UPDATE WX_GH set accessToken=? , expiredTime=? WHERE appID=?", token, new Date(time), key)
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
    wx.find("SELECT * FROM WX_GH").forEach {
      iter => {
        val ac = new ApiConfig()
        ac.setToken(iter.getStr("TOKEN"))
        ac.setAppId(iter.getStr("appID"))
        ac.setAppSecret(iter.getStr("appSecret"))
        ApiConfigKit.putApiConfig(ac)
      }
    }
  }
}
