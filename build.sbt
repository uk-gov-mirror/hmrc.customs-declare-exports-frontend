import TestPhases.oneForkedJvmPerTest
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "customs-declare-exports-frontend"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test(),
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .settings(
    publishingSettings: _*
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest) (base => Seq(base / "it")).value,
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false,
    addTestReportOption(IntegrationTest, "int-test-reports")
  )
  .settings(
      resolvers += "Sonatype Repo" at "http://oss.sonatype.org/content/groups/public/"
  )
