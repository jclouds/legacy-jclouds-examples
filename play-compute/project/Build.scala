import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "play-compute"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "com.google.guava" % "guava" % "12.0",
      "org.jclouds" % "jclouds-compute" % "1.5.0-alpha.6",
      "org.reflections" % "reflections" % "0.9.7.RC1"
      //If using the securesocial module you may need to do the following to make jclouds run:
      //"securesocial" %% "securesocial" % "2.0.12" excludeAll(ExclusionRule(organization="org.ow2.spec.ee"), ExclusionRule(organization="com.cedarsoft"))
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      resolvers += "Maven central" at "http://repo1.maven.org/maven2/",
      resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    )

}
