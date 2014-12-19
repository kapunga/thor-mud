package org.kapunga.tm.soul

import org.kapunga.tm.world.SpawnPoint
import org.anormcypher._
import org.anormcypher.CypherParser._

/**
 * A Soul is the base user entity for ThorMUD.  It is basically a spirit of varying power
 * that can have one or more Avatars that can play the game.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 *
 * @param name The username of this Avatar
 * @param password The password for this Soul.  This should be hashed before it reaches this.
 * @param email The email of this Soul.  Used to reset passwords, etc.
 * @param id The Neo4j node id of this soul.
 */
// TODO Make name checking also check reserved words.
// TODO Move spawn points to spirits and avatars.
case class Soul(name: String, password: String, email: String, id: Int = -1) {
  def spawnPoint = SpawnPoint.globalSpawn
}

/**
 * A companion object for a Soul, it acts as a DAO for the soul.
 */
// TODO Put some kind of restrictions on database access.
object Soul {
  implicit val connection = Neo4jREST()

  val soulParser: CypherRowParser[Soul] = {
    str("name") ~ str("password") ~ str("email") ~ int("id") map {
      case a ~ b ~ c ~ d => Soul(a, b, c, d)
    }
  }

  /**
   * Lookup a Soul by it's name.
   *
   * @param soulName The name of the soul to look up.
   * @return An Option, populated with a soul if it exists, otherwise None.
   */
  def findByName(soulName: String): Option[Soul] = {
    val query = Cypher(s"MATCH (soul :Soul {name:'$soulName'}) " +
      "RETURN id(soul) AS id, soul.name AS name, soul.password AS password, soul.email AS email;")
    query.singleOpt(soulParser)
  }

  /**
   * Creates a new Soul in the database.
   *
   * @param name The new Soul's name.
   * @param password The new Soul's encrypted password.
   * @param email The new Soul's email address.
   * @return True if we were successful, otherwise false.
   */
  def createSoul(name: String, password: String, email: String): Option[Soul] = {
    Cypher(s"CREATE (soul :Soul {name:'$name', password:'$password', email:'$email'}) RETURN soul;").execute() match {
      case true => findByName(name)
      case false => None
    }
  }

  /**
   * Check if a name is available.  Ignores case.
   *
   * @param name The name to check.
   * @return True if the name is not in the database, true if it is.
   */
  def nameAvailable(name: String): Boolean = available("name", name)

  /**
   * Check if an email is available.  Ignores case.
   *
   * @param email The email address to check.
   * @return True if the email address is not in the database, true if it is.
   */
  def emailAvailable(email: String): Boolean = available("email", email)

  private def available(tag: String, value: String): Boolean = {
    val query = Cypher(s"OPTIONAL MATCH (n :Soul) WHERE n.$tag =~ '(?i)$value' RETURN COUNT(n) = 0 AS a;")
    query.as[Boolean](CypherParser.bool("a").single)
  }
}
