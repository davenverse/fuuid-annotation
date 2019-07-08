lazy val `fuuid-annotation` = project
  .in(file("."))
  .settings(commonSettings, releaseSettings, skipOnPublishSettings)
  .aggregate(core, doobie, docs)

lazy val core = project
  .in(file("core"))
  .settings(scalacOptions --= Seq("-Ywarn-unused:patvars"))
  .settings(commonSettings, releaseSettings, mimaSettings)
  .settings(name := "fuuid-annotation")
  .settings(libraryDependencies ++= Seq(
    "org.scala-lang"    % "scala-reflect"          % scalaVersion.value,
    "com.chuusai"       %% "shapeless"             % shapelessV % Test
  ))

lazy val doobie = project
  .in(file("doobie"))
  .settings(scalacOptions --= Seq("-Ywarn-unused:patvars"))
  .settings(commonSettings, releaseSettings, mimaSettings)
  .settings(name := "fuuid-annotation-doobie")
  .settings(Defaults.itSettings)
  .configs(IntegrationTest)
  .dependsOn(core)
  .settings(libraryDependencies ++= Seq(
    "com.chuusai"       %% "shapeless"             % shapelessV % IntegrationTest,
    "io.chrisdavenport" %% "fuuid-doobie"          % fuuidV % IntegrationTest,
    "org.specs2"        %% "specs2-cats"           % specs2V % IntegrationTest,
    "org.tpolecat"      %% "doobie-core"           % doobieV % IntegrationTest,
    "org.tpolecat"      %% "doobie-postgres"       % doobieV % IntegrationTest,
    "io.chrisdavenport" %% "testcontainers-specs2" % testContainersSpecs2V % IntegrationTest
  ))

lazy val docs = project
  .in(file("docs"))
  .settings(commonSettings, skipOnPublishSettings, micrositeSettings)
  .dependsOn(core)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(TutPlugin)
  .settings(libraryDependencies ++= Seq(
    "io.chrisdavenport" %% "fuuid-doobie"    % fuuidV,
    "org.tpolecat"      %% "doobie-postgres" % doobieV,
  ))

lazy val contributors = Seq(
  "ChristopherDavenport" -> "Christopher Davenport",
  "alejandrohdezma" -> "Alejandro Hernández"
)

val doobieV = "0.7.0"
val shapelessV = "2.3.3"
val fuuidV = "0.2.0"
val specs2V = "4.6.0"
val macroParadiseV = "2.1.1"
val silencerV = "1.4.1"
val testContainersSpecs2V = "0.2.0-M1"

// General Settings
lazy val commonSettings = Seq(
  organization := "io.chrisdavenport",
  scalaVersion := "2.12.8",
  crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),
  scalacOptions += "-Yrangepos",
  scalacOptions in (Compile, doc) ++= Seq(
    "-groups",
    "-sourcepath",
    (baseDirectory in LocalRootProject).value.getAbsolutePath,
    "-doc-source-url",
    "https://github.com/ChristopherDavenport/fuuid-annotation/blob/v" + version.value + "€{FILE_PATH}.scala"
  ),
  addCompilerPlugin("org.scalamacros" % "paradise"         % macroParadiseV cross CrossVersion.full),
  addCompilerPlugin("com.github.ghik" %% "silencer-plugin" % silencerV),
  libraryDependencies ++= Seq(
    "com.github.ghik"   %% "silencer-lib" % silencerV % Provided,
    "io.chrisdavenport" %% "fuuid"        % fuuidV,
    "org.specs2"        %% "specs2-core"  % specs2V % Test,
    "org.specs2"        %% "specs2-cats"  % specs2V % Test
  )
)

lazy val releaseSettings = {
  import ReleaseTransformations._
  Seq(
    releaseCrossBuild := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      // For non cross-build projects, use releaseStepCommand("publishSigned")
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials ++= (
      for {
        username <- Option(System.getenv().get("SONATYPE_USERNAME"))
        password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
      } yield
        Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          username,
          password
        )
    ).toSeq,
    publishArtifact in Test := false,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ChristopherDavenport/fuuid-annotation"),
        "git@github.com:ChristopherDavenport/fuuid-annotation.git"
      )
    ),
    homepage := Some(url("https://github.com/ChristopherDavenport/fuuid-annotation")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := true,
    pomIncludeRepository := { _ =>
      false
    },
    pomExtra := {
      <developers>
        {for ((username, name) <- contributors) yield
        <developer>
          <id>{username}</id>
          <name>{name}</name>
          <url>http://github.com/{username}</url>
        </developer>
        }
      </developers>
    }
  )
}

lazy val mimaSettings = {
  import sbtrelease.Version

  def semverBinCompatVersions(major: Int, minor: Int, patch: Int): Set[(Int, Int, Int)] = {
    val majorVersions: List[Int] =
      if (major == 0 && minor == 0) List.empty[Int] // If 0.0.x do not check MiMa
      else List(major)
    val minorVersions: List[Int] =
      if (major >= 1) Range(0, minor).inclusive.toList
      else List(minor)
    def patchVersions(currentMinVersion: Int): List[Int] =
      if (minor == 0 && patch == 0) List.empty[Int]
      else if (currentMinVersion != minor) List(0)
      else Range(0, patch - 1).inclusive.toList

    val versions = for {
      maj <- majorVersions
      min <- minorVersions
      pat <- patchVersions(min)
    } yield (maj, min, pat)
    versions.toSet
  }

  def mimaVersions(version: String): Set[String] = {
    Version(version) match {
      case Some(Version(major, Seq(minor, patch), _)) =>
        semverBinCompatVersions(major.toInt, minor.toInt, patch.toInt)
          .map { case (maj, min, pat) => maj.toString + "." + min.toString + "." + pat.toString }
      case _ =>
        Set.empty[String]
    }
  }
  // Safety Net For Exclusions
  lazy val excludedVersions: Set[String] = Set()

  // Safety Net for Inclusions
  lazy val extraVersions: Set[String] = Set()

  Seq(
    mimaFailOnProblem := mimaVersions(version.value).toList.nonEmpty,
    mimaPreviousArtifacts := (mimaVersions(version.value) ++ extraVersions)
      .diff(excludedVersions)
      .map { v =>
        val moduleN = moduleName.value + "_" + scalaBinaryVersion.value.toString
        organization.value % moduleN % v
      },
    mimaBinaryIssueFilters ++= {
      Seq()
    }
  )
}

lazy val micrositeSettings = {
  import microsites._
  Seq(
    micrositeName := "fuuid-annotation",
    micrositeDescription := "Annotated FUUID's",
    micrositeAuthor := "Christopher Davenport",
    micrositeGithubOwner := "ChristopherDavenport",
    micrositeGithubRepo := "fuuid-annotation",
    micrositeBaseUrl := "/fuuid-annotation",
    micrositeDocumentationUrl := "https://www.javadoc.io/doc/io.chrisdavenport/fuuid-annotation_2.12",
    micrositeGitterChannelUrl := "christopherdavenport/libraries",
    micrositeFooterText := None,
    micrositeHighlightTheme := "atom-one-light",
    micrositePalette := Map(
      "brand-primary" -> "#3e5b95",
      "brand-secondary" -> "#294066",
      "brand-tertiary" -> "#2d5799",
      "gray-dark" -> "#49494B",
      "gray" -> "#7B7B7E",
      "gray-light" -> "#E5E5E6",
      "gray-lighter" -> "#F4F3F4",
      "white-color" -> "#FFFFFF"
    ),
    fork in tut := true,
    scalacOptions in Tut --= Seq(
      "-Xfatal-warnings",
      "-Ywarn-unused-import",
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Ywarn-unused:imports",
      "-Xlint:-missing-interpolator,_"
    ),
    libraryDependencies += "com.47deg" %% "github4s" % "0.20.1",
    micrositePushSiteWith := GitHub4s,
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
    micrositeExtraMdFiles := Map(
      file("CHANGELOG.md") -> ExtraMdFileConfig(
        "changelog.md",
        "page",
        Map("title" -> "changelog", "section" -> "changelog", "position" -> "100")),
      file("CODE_OF_CONDUCT.md") -> ExtraMdFileConfig(
        "code-of-conduct.md",
        "page",
        Map("title" -> "code of conduct", "section" -> "code of conduct", "position" -> "101")),
      file("LICENSE") -> ExtraMdFileConfig(
        "license.md",
        "page",
        Map("title" -> "license", "section" -> "license", "position" -> "102"))
    )
  )
}

lazy val skipOnPublishSettings = Seq(
  skip in publish := true,
  publish := (),
  publishLocal := (),
  publishArtifact := false,
  publishTo := None
)
