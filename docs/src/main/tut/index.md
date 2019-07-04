---
layout: home

---

# fuuid-annotation - Annotated FUUID's [![Build Status](https://travis-ci.com/ChristopherDavenport/fuuid-annotation.svg?branch=master)](https://travis-ci.com/ChristopherDavenport/fuuid-annotation) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/fuuid-annotation_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/fuuid-annotation_2.12)

Automatically create an inner `Id` tagged `FUUID` type with convenient methods for its creation.

## Quick Start

To use fuuid-annotation in an existing SBT project with Scala 2.11 or a later version, add the following dependencies to your
`build.sbt` depending on your needs:

```scala
libraryDependencies ++= Seq(
  "io.chrisdavenport" %% "fuuid-annotation" % "<version>"
)
```

Then, annotate your model's companion object:

```tut:silent
import io.chrisdavenport.fuuid.annotation.DeriveId
import io.chrisdavenport.fuuid.FUUID
import cats.effect.IO

@DeriveId
object User
```

Now the type `User.Id` is available and its companion object contains convenient methods for its creation:

- `apply` method receiving a valid `FUUID`:

```tut
User.Id(FUUID.fuuid("bd9686b6-efcd-434c-b4f4-f46b990c1808"))
```

- `apply` method receiving a valid `UUID` literal (uses a macro to compile-check the literal value):

```tut
User.Id("bd9686b6-efcd-434c-b4f4-f46b990c1808")
```

- `random` method that generates a new `Id` wrapped in an `F`

```tut
User.Id.random[IO]
```

- `Unsafe.random` method that generates a new `Id` unwrapped. This is an unsafe creation, it should only be used on a controlled environment, like tests:

```tut
User.Id.Unsafe.random
```

The annotation also provides instances for cats `Show`, `Order` and `Hash` (availables at `User._`)

## Doobie integration

`DeriveId` can also auto-generate a [doobie's Meta](https://git.io/fj64d) instance to use the generated `Id` type in doobie's queries.

You'll need to add the following dependencies to your `build.sbt`:
                  
```scala
libraryDependencies ++= Seq(
  "io.chrisdavenport" %% "fuuid"           % "<version>",
  "org.tpolecat"      %% "doobie-postgres" % "<doobie-version>",
)
```

Then, just set annotation's `deriveMeta` to `true`:

```tut:silent
@DeriveId(deriveMeta = true)
object Person
``` 