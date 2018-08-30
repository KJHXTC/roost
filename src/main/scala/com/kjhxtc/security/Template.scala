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

package com.kjhxtc.security

import scala.collection.mutable

private[security] trait Provider {
  def getName: String
}

private[security] trait Template[T <: Provider] {

  private[this] val providers: mutable.HashSet[T] = mutable.HashSet()

  def add(c: T): Unit = {
    if (null == c || null == c.getName) throw new IllegalArgumentException("PasswordHelper should be nonEmpty")
    if (providers.exists(p => p.getName == c.getName)) throw new IllegalStateException("Provider already exists")
    providers.add(c)
  }

  def apply(provider: String): T = {
    providers.foreach(p =>
      if (p.getName == provider) return p
    )
    throw new IllegalStateException("No Such Provider: " + provider)
  }

  def apply(): T = {
    providers.headOption.getOrElse(throw new IllegalStateException("No Any Provider"))
  }
}
