name := "Thor MUD"
 
version := "1.0"
  
scalaVersion := "2.11.4"
   
resolvers ++= Seq(
  "anormcypher" at "http://repo.anormcypher.org/",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-daemon" % "1.0.9" from "http://repo1.maven.org/maven2/org/apache/commons/commons-daemon/1.0.9/commons-daemon-1.0.9.jar",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4", // Akka actors and IO
  "org.mindrot" % "jbcrypt" % "0.3m",            // Encryption library for hashing passwords
  "org.anormcypher" %% "anormcypher" % "0.6.0",   // Library to connect to neo4j
  "ch.qos.logback" % "logback-classic" % "1.1.2"
)

