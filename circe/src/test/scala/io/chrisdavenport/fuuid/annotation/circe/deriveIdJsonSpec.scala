package io.chrisdavenport.fuuid.annotation.circe

import io.chrisdavenport.fuuid.annotation.deriveId
import io.chrisdavenport.fuuid.circe._
import org.specs2.mutable.Specification

import io.circe.syntax._
import io.circe.literal._

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.NonUnitStatements"
  )
)
class deriveIdJsonSpec extends Specification {

  @deriveId @deriveIdJson
  object User

  "User.Id" should {

    "be decoded from JSON" >> {
      val id = User.Id.Unsafe.random

      val json = json"""${id.show}"""

      json.as[User.Id] must beRight(id)
    }

    "be encoded to JSON" >> {
      val id = User.Id.Unsafe.random

      val json = json"""${id.show}"""

      id.asJson must be equalTo json
    }

  }

}
