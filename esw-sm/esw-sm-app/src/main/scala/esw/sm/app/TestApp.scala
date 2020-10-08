package esw.sm.app

import com.typesafe.config.ConfigFactory

object TestApp extends App {
  private val value: String = ConfigFactory.load().getString("foo.bar")
  println(s"*********** value **************** $value")
}
