import AssemblyKeys._

name := "Thor MUD"
 
version := "1.0"
  
scalaVersion := "2.11.4"
   
resolvers ++= Seq(
  "anormcypher" at "http://repo.anormcypher.org/",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",        // Akka actors and IO
  "org.mindrot" % "jbcrypt" % "0.3m",                   // Encryption library for hashing passwords
  "org.anormcypher" %% "anormcypher" % "0.6.0",         // Library to connect to neo4j
  "ch.qos.logback" % "logback-classic" % "1.1.2",       // SLF4J Wrapper library for logging.
  "com.typesafe" % "config" % "1.2.1",                  // Typesafe config library.
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test" // ScalaTest unit testing framework
)

assemblySettings
