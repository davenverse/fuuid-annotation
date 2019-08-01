package io.chrisdavenport.fuuid.annotation

import cats.Show
import cats.effect.IO
import cats.kernel.{Eq, Hash, Order}
import io.chrisdavenport.fuuid.{FUUID => Fuuid}
import org.specs2.matcher.IOMatchers
import org.specs2.mutable.Specification
import shapeless.test.illTyped

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.NonUnitStatements"
  )
)
class DeriveIdSpec extends Specification with IOMatchers {

  @DeriveId
  object User

  "FUUID annotation" should {

    import User._

    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    val expected = Fuuid.fuuid("13ea2ea9-6e30-4160-8491-f8d900eadb8f")

    "only be used with objects" >> {
      illTyped("""@DeriveId class Thing""", "@DeriveId can only be used with objects")

      success
    }

    "create apply FUUID method that" should {

      "create Id out of a FUUID" >> {
        @SuppressWarnings(Array("org.wartremover.warts.Throw"))
        val fuuid = Fuuid.fuuid("13ea2ea9-6e30-4160-8491-f8d900eadb8f")

        val userId = User.Id.apply(fuuid)

        userId must beEqualTo(expected)
      }

    }

    "create apply macro UUID method that" should {

      "fail when not UUID" >> {
        illTyped("""User.Id.apply("miau")""", "Invalid FUUID string: miau")

        success
      }

      "fail when not literal" >> {
        illTyped(
          """val string = "miau"; User.Id.apply(string)""",
          "This method uses a macro to verify that a FUUID literal is valid." +
            " Use FUUID.fromString if you have a dynamic value you want to" +
            " parse as an FUUID."
        )

        success
      }

      "compile when valid UUID" >> {
        val userId = User.Id.apply("13ea2ea9-6e30-4160-8491-f8d900eadb8f")

        userId must beEqualTo(expected)
      }

    }

    "create random method that" should {

      "create random Id value wrapped in an F" >> {
        val io = User.Id.random[IO]

        io must returnValue(anInstanceOf[Fuuid])
      }

    }

    "create Unsafe.random method that" should {

      "create random Id value unwrapped" >> {
        val userId = User.Id.Unsafe.random

        userId must beAnInstanceOf[Fuuid]
      }

    }

    "create implicit Hash[Id] instance" >> {
      val id = User.Id(expected)

      Hash[User.Id].hash(id) must beEqualTo(expected.hashCode)
    }

    "create implicit Eq[Id] instance" >> {
      val id = User.Id("13ea2ea9-6e30-4160-8491-f8d900eadb8f")

      Eq[User.Id].eqv(id, id) must beTrue
    }

    "create implicit Order[Id] instance" >> {
      val id = User.Id("13ea2ea9-6e30-4160-8491-f8d900eadb8f")

      Order[User.Id].compare(id, id) must beEqualTo(0)
    }

    "create implicit Show[Id] instance" >> {
      val id = User.Id("13ea2ea9-6e30-4160-8491-f8d900eadb8f")

      Show[User.Id].show(id) must beEqualTo(expected.show)
    }

  }

}
