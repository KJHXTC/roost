package com.kjhxtc.mwemxa.Model

import com.jfinal.plugin.activerecord.Model

class WechatUser extends Model[WechatUser] {

  def findOpenId(id: String): Option[WechatUser] = {
    val u = dao.findFirst(s"select * from WECHAT_USER where OPENID=?", id)
    Option(u)
  }

  def bindOpenIDwithUserID(id:BigInt): Unit ={
    set("UID", id)
    save()
  }
}
