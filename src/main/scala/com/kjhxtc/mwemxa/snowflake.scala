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

  def init(agentId: Int, cluserID: Int): Unit = {
    worker = new IdWorker(agentId, cluserID)
  }

  def getId(ua: String): Long = worker.get_id(ua)

  def validUA(ua: String): Boolean = worker.validUseragent(ua)
}
