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


import com.jfinal.weixin.sdk.msg.in.InMsg
import com.jfinal.weixin.sdk.msg.out.OutMsg

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * 限时应答 按照微信协议在成功接收到信息后的 5S内 给与响应
  * 否则将会触发微信消息重发
  * 又因JFinal的weixin api无排重MsgId接口所以最好在 5S内反应
  */
trait TimeLimitTask {

  def within(in: InMsg)(c: () => OutMsg): Unit = {
    val ct = System.currentTimeMillis()
    val f = Future[OutMsg](c())

    Await.ready(f, Duration.Inf) flatMap {
      value =>
        Future {
          val ce = System.currentTimeMillis()
          if (ce - ct > 4500) {

          } else {
            new AsyncReply().reply(in)
          }
        }
    }
  }
}
