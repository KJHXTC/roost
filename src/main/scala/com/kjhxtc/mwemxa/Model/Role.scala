package com.kjhxtc.mwemxa.Model

import com.jfinal.plugin.activerecord.Model

sealed abstract class Permission

case object SYS_ADMIN extends Permission

case object SYS_OPERT extends Permission

case object SYS_MODSE extends Permission

case object USER_GPE extends Permission

case object USER_NOR extends Permission


class Role extends Model[Role] {
  /**
    * role_ID, role_NAME, role_DESC
    */


}
