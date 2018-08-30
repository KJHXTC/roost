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

import com.jfinal.plugin.activerecord.Record

abstract class DBOps(tableName: String, ignoreCase: Boolean = true) {

  var record: Record = _

  def get[T](key: String, defaultValue: AnyRef = null): T = {
    record.get(key, defaultValue)
  }

  def set(key: String, value: AnyRef): this.type = {
    if (record != null) {
      record.set(key, value)
    } else {
      record = new Record()
      record.set(key, value)
    }
    this
  }
}