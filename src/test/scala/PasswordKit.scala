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

import com.kjhxtc.mwemxa.ISHelper
import com.kjhxtc.security.AuthenticateHelper
import javax.crypto.Cipher

object PasswordKit extends App with ISHelper {
  val cipher = "2itVSn7Lizh4BDbxPv8EWA=="
  val secureKey = AuthenticateHelper.testKey
  val aes = Cipher.getInstance("AES/ECB/PKCS5Padding")
  aes.init(Cipher.DECRYPT_MODE, secureKey)
  val key = aes.doFinal(decode(cipher))
  println(new String(key))
}
