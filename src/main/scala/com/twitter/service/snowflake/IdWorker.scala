/** Copyright 2010-2012 Twitter, Inc. */
package com.twitter.service.snowflake

import java.util.Random

import com.kjhxtc.mwemxa.Logger

class InvalidUserAgentError extends IllegalArgumentException

class InvalidSystemClock(msg: String) extends IllegalStateException(msg)

/**
  * An object that generates IDs.
  * This is broken into a separate class in case
  * we ever want to support multiple worker threads
  * per process
  */
class IdWorker(val workerId: Long, val datacenterId: Long, var sequence: Long = 0L)
  extends Logger {

  val twepoch = 1288834974657L
  val AgentParser = """([a-zA-Z].*)""".r
  private[this] val rand = new Random
  private[this] val workerIdBits = 5L
  private[this] val datacenterIdBits = 5L
  private[this] val maxWorkerId = -1L ^ (-1L << workerIdBits)
  private[this] val maxDatacenterId = -1L ^ (-1L << datacenterIdBits)
  private[this] val sequenceBits = 12L
  private[this] val workerIdShift = sequenceBits
  private[this] val datacenterIdShift = sequenceBits + workerIdBits
  private[this] val timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits
  private[this] val sequenceMask = -1L ^ (-1L << sequenceBits)


  log info ("worker starting. timestamp left shift %d, datacenter id bits %d, worker id bits %d, sequence bits %d, workerid %d" format
    (timestampLeftShift, datacenterIdBits, workerIdBits, sequenceBits, workerId))
  private[this] var lastTimestamp = -1L

  def get_id(useragent: String): Long = {
    if (!validUseragent(useragent)) {
      throw new InvalidUserAgentError
    }

    val id = nextId()

    id
  }

  protected[snowflake] def nextId(): Long = synchronized {
    var timestamp = timeGen()

    if (timestamp < lastTimestamp) {
      log.error("clock is moving backwards.  Rejecting requests until %d." format lastTimestamp)
      throw new InvalidSystemClock("Clock moved backwards.  Refusing to generate id for %d milliseconds".format(
        lastTimestamp - timestamp))
    }

    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & sequenceMask
      if (sequence == 0) {
        timestamp = tilNextMillis(lastTimestamp)
      }
    } else {
      sequence = 0
    }

    lastTimestamp = timestamp
    ((timestamp - twepoch) << timestampLeftShift) |
      (datacenterId << datacenterIdShift) |
      (workerId << workerIdShift) |
      sequence
  }

  protected def tilNextMillis(lastTimestamp: Long): Long = {
    var timestamp = timeGen()
    while (timestamp <= lastTimestamp) {
      timestamp = timeGen()
    }
    timestamp
  }

  protected def timeGen(): Long = System.currentTimeMillis()

  def validUseragent(useragent: String): Boolean = useragent match {
    case AgentParser(_) => true
    case _ => false
  }

  def get_worker_id(): Long = workerId

  def get_datacenter_id(): Long = datacenterId

  def get_timestamp(): Long = System.currentTimeMillis
}
