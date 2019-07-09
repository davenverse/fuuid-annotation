package io.chrisdavenport.fuuid.annotation.doobie

import java.util.UUID

import cats.effect.IO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.annotation.deriveId
import io.chrisdavenport.fuuid.doobie.implicits._
import io.chrisdavenport.testcontainersspecs2.{ForAllTestContainer, UsesPostgreSQLMultipleDatabases}
import org.specs2.matcher.IOMatchers
import org.specs2.mutable.Specification

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.NonUnitStatements"
  )
)
class deriveIdMetaSpec
    extends Specification
    with UsesPostgreSQLMultipleDatabases
    with ForAllTestContainer
    with IOMatchers {

  lazy val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    driverName,
    jdbcUrl,
    dbUserName,
    dbPassword
  )

  @deriveId
  @deriveIdMeta
  object User

  "Meta[User.Id]" should {

    "return User.Id on UUID" in {
      val uuid = UUID.randomUUID()

      val io = sql"SELECT $uuid::uuid"
        .query[User.Id]
        .unique
        .transact(transactor)

      io must returnValue(User.Id(FUUID.fromUUID(uuid)))
    }

    "insert User.Id as uuid" in {
      val id = User.Id.Unsafe.random

      val io = sql"SELECT $id::uuid"
        .query[String]
        .unique
        .transact(transactor)

      io must returnValue(id.toString)
    }

  }

}
