package org.kapunga.tm.soul

import org.kapunga.tm.soul.Spirit._
import org.kapunga.tm.world.Pantheon
import org.scalatest.FlatSpec

class SpiritSpec extends FlatSpec {
  "A new Spirit" should "default to Whisper of every key if no default is specified" in {
    val testSpirit = new Spirit()

    assertAllLevelsEqual(testSpirit, Whisper)
  }

  it should "return the default level for empty pantheons if a default is specified" in {
    val testSpirit = new Spirit(Creator)

    assertAllLevelsEqual(testSpirit, Creator)
  }

  "A Spirit" should "not allow demotion below Whisper" in {
    val testSpirit = new Spirit()

    assert(!testSpirit.demote())
  }

  it should "not allow promotion above Almighty" in {
    val testSpirit = new Spirit(Almighty)

    assert(!testSpirit.promote())
  }

  it should "raise the level globally if no Pantheon is specified" in {
    val testSpirit = new Spirit()

    testSpirit.promote()

    assertAllLevelsEqual(testSpirit, Ghost)
  }

  it should "raise only the level for a specific pantheon if specified" in {
    val testSpirit = new Spirit()

    assert(testSpirit.promote(testPantheon(1)))

    assert(testSpirit.spiritLevel(testPantheon(1)) == Ghost)

    assertAllLevelsEqual(testSpirit, Whisper, except = 1 :: Nil)
  }

  it should "lower the level globally if no Pantheon is specified" in {
    val testSpirit = new Spirit(Ghost)

    assertAllLevelsEqual(testSpirit, Ghost)

    testSpirit.demote()

    assertAllLevelsEqual(testSpirit, Whisper)
  }

  it should "lower only the level for a specific pantheon if specified" in {
    val testSpirit = new Spirit(Ghost)

    testSpirit.promote(testPantheon(1))
    testSpirit.promote(testPantheon(1))

    assert(testSpirit.spiritLevel(testPantheon(1)) == DemiGod)

    assertAllLevelsEqual(testSpirit, Ghost, except = 1 :: Nil)

    assert(testSpirit.demote(testPantheon(1)))

    assert(testSpirit.spiritLevel(testPantheon(1)) == Angel)

    assertAllLevelsEqual(testSpirit, Ghost, except = 1 :: Nil)
  }

  it should "not lower the level for a specific level below the default level" in {
    val testSpirit = new Spirit(Ghost)

    assert(!testSpirit.demote(testPantheon(1)))

    assertAllLevelsEqual(testSpirit, Ghost)
  }

  // TODO Add a few more tests.

  private def assertAllLevelsEqual(spirit: Spirit, spiritLevel: Spirit.Value, except: List[Int] = Nil) = {
    -1 until 10 filter (i => !except.contains(i)) foreach {pId =>
      assert(spirit.spiritLevel(testPantheon(pId)) == spiritLevel)
    }
  }

  private def testPantheon(id: Int) = new Pantheon(id, "Test pantheon", "Test pantheon")
}
