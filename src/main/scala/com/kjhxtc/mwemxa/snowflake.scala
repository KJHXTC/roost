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

import com.twitter.service.snowflake.IdWorker

/**
  * - - timestamp(mills)                              - ClusterID - WorkerID  - seq
  * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000     - 00000     - 000000000000
  *
  * @see `<a href="https://github.com/twitter/snowflake">Snowflake</a>`
  */
object snowflake {
  private[this] var worker: IdWorker = _

  def init(agentId: Int, clusterId: Int): Unit = {
    worker = new IdWorker(agentId, clusterId)
  }

  def getId(ua: String): Long = worker.get_id(ua)

  def validUA(ua: String): Boolean = worker.validUseragent(ua)
}
