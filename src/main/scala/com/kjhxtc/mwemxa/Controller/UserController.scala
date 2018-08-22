package com.kjhxtc.mwemxa.Controller

import java.sql.Timestamp

import com.kjhxtc.mwemxa.Model._
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonAST.{JField, JInt, JObject, JString}

class UserController extends JsonServiceCtrl {
  override def data: JsonAST.JValue = {
    val uid = try {
      getParaToInt("uid")
    } catch {
      case _: NumberFormatException =>
        return JObject(
          JField("status", JInt(400)),
          JField("message", JString("INVALID PARAMS"))
        )
    }

    val user = new User()
    val profile = new Profile()
    val c = user.dao().findById(uid)
    if (c == null) return JObject(
      JField("status", JInt(404)),
      JField("message", JString("USER NOT FOUND"))
    )
    val d = profile.dao().findFirst(s"select * from profile where G_UUID='${c.get("G_UUID").toString}'")
    if (d == null) return JObject(
      JField("status", JInt(500)),
      JField("message", JString("USER NOT FOUND"))
    )
    JObject(
      JField("login", JString(c.get("LOGIN"))),
      JField("create_time", JString(c.getTimestamp("CREATE_TIME").toString)),
      JField("name", JString(d.get("NAME")))
    )
  }
}