package com.kjhxtc.mwemxa.Controller

import com.kjhxtc.mwemxa.Logger
import com.kjhxtc.mwemxa.Model.User

import scala.collection.mutable

object UserCache extends Logger{
  //default way

  // 缓存池 有条件 可以重写 使用memory cached 或者 redis 存储并追加清理时间 (1小时)
  private val cache: mutable.WeakHashMap[String, User] = new mutable.WeakHashMap[String, User]()

  def get(key: String): Option[User] = {
    val u = cache.get(key)
    log debug s"Select $key User " + u
    u
  }

  def set(key: String, value: User): Unit = {
    log debug "Cache Session " + key
    cache.put(key, value)
  }
}
