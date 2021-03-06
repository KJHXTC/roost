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

package com.kjhxtc.mwemxa.Model

import com.jfinal.plugin.activerecord.Model

class WechatUser extends Model[WechatUser] {

  def findOpenId(id: String): Option[WechatUser] = {
    val u = dao.findFirst(s"select * from WX_USER where OPENID=?", id)
    Option(u)
  }

  def bindOpenIDwithUserID(id: BigInt): Unit = {
    set("UID", id)
    save()
  }
}
