package com.kjhxtc.mwemxa

import com.twitter.service.snowflake.IdWorker

/**
  * - - timestamp(mills)                              - ClusterID - WorkerID  - seq
  * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000     - 00000     - 000000000000
  *
  * @see `<a href="https://github.com/twitter/snowflake">Snowflake</a>`
  */
object snowflake {
  val gen = new IdWorker(1, 1)
}
