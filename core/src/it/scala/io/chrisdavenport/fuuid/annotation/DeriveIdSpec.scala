package io.chrisdavenport.fuuid.annotation

import java.util.UUID

import cats.effect.IO
import com.dimafeng.testcontainers.Container
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.testcontainersspecs2.{ForAllTestContainer, PostgresqlMultipleDatabases}
import org.specs2.matcher.IOMatchers
import org.specs2.mutable.Specification

class DeriveIdSpec extends Specification with ForAllTestContainer with IOMatchers {

  private[this] val multiple = new PostgresqlMultipleDatabases(
    name = "christopherdavenport/postgres-multi-db:10.3",
    exposedPort = 5432,
    dbName = dbName,
    dbUserName = dbUserName,
    dbPassword = dbPassword
  )

  lazy val driverName: String = "org.postgresql.Driver"
  lazy val dbUserName: String = "banno_db"
  lazy val dbPassword: String = "password"
  lazy val dbName: String = "db"
  lazy val jdbcUrl: String = multiple.jdbcUrl

  lazy val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    driverName,
    jdbcUrl,
    dbUserName,
    dbPassword
  )

  override def container: Container = multiple.container

  @DeriveId(deriveMeta = true)
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
