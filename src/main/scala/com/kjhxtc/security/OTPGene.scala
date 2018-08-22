package com.kjhxtc.security

import java.security.SecureRandom

import org.jboss.aerogear.security.otp.Totp
import org.jboss.aerogear.security.otp.api.Base32

object OTPGene {

  def genOtp(id: String, token: String): String = {
    new SoftOtpGene(token).genOtp
  }

  def verifyOtp(token: String, otp: String): Boolean = {
    new SoftOtpGene(token).verify(otp)
  }

  def randomSecure(): String = {
    val key = new Array[Byte](16)
    new SecureRandom().nextBytes(key)
    Base32.encode(key)
  }
}

class SoftOtpGene(secret: String) {
  val c = new Totp(secret)

  def genOtp: String = c.now()

  def verify(code: String): Boolean = c.verify(code)

}
