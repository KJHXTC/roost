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

package com.kjhxtc.security.policy

import java.util.Date

import com.jfinal.plugin.activerecord.{Db, IAtom, Record}
import com.kjhxtc.mwemxa.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

sealed abstract class OpTypes(ident: String) {
  override def toString: String = ident.toUpperCase
}


/**
  * 涉及审计的重要操作记录到日志中,核心是
  * 系统时间(Time) 地点(IP) 人物(名称ID) 事件(动作Action)  结果(影响Content)
  * 还有数据库私有的最后修改时间
  */
object auditRecord extends Logger {

  object LOGIN extends OpTypes("LOGIN")

  object LOGOUT extends OpTypes("LOGOUT")

  object REGISTER extends OpTypes("REGISTER")

  object DESTROY extends OpTypes("UNREGISTER")

  object MODIFY_PASSWORD extends OpTypes("PASSWORD CHANGE")

  object RESET_PASSWORD extends OpTypes("PASSWORD FIND")

  /**
    * 记录日志到默认数据库 (这个数据量会因用户操作频繁度和用户数量的增加而陡增)
    * 初期可以写入数据库 后期建议单独分离记录到例如MongoDB数据库中
    *
    * @param opDate  操作时的那台主机的系统时间(UTC)
    * @param address 操作时来源的主机地址,包括直接地址socket.Peer, 声明地址 X_REAL_IP等
    * @param uid     操作的账号
    * @param name    当时的登录名
    * @param term    操作的终端标识
    * @param action  操作的动作,常规的参见{ @see xxx}
    * @param result  操作的结果
    * @param src     记录日志的程序埋点
    */
  def record(opDate: Date, address: String, uid: Long, name: String, term: String, action: String, result: String, src: String): Unit = {
    val op = new Record()
      .set("UID", uid)
      .set("NAME", name) //当时的登录名 -- 可能会修改 当实名登录 有用
      .set("DATE", opDate)
      .set("DEVICE", term.slice(0, 63))
      .set("ACTION", action)
      .set("CONTENT", result.slice(0, 255))
      .set("ADDRESS", address) //操作发起者的IP记录
      .set("EVENT_ID", src.slice(0, 20)) //程序锚点
    Future {
      Db.tx(new IAtom {
        override def run(): Boolean = {
          Db.save("TB_LOGS", op)
        }
      })
    } onComplete {
      case Success(value) =>
        if (!value) {
          // TODO 失败记录如何后期添加到日志中
          log error "RECORD ERROR DOTI LATER::" + op.toString
        }
      case Failure(exception) =>
        // TODO 失败记录如何后期添加到日志中
        exception.printStackTrace()
        log error "RECORD ERROR DOTI LATER::" + op.toString
    }
  }
}
