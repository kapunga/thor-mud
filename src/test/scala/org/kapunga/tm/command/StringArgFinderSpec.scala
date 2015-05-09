package org.kapunga.tm.command

import org.scalatest.FlatSpec

class StringArgFinderSpec extends FlatSpec {
  val stringSet = Set("s", "say", "south", "tell", "whisper", "who")

  val argFinder = new StringArgFinder(() => stringSet)

  // An empty context is fine as this ArgFinder is not context dependent.
  val emptyContext = new ExecContext(null, null)

  // getArg specs
  "StringArgFinder.getArg method" should "return an empty string and no remainder on an empty input" in {
    val result = argFinder.getArg("", emptyContext)

    assert(result == (Some(""), ""))
  }

  it should "return an empty string on no match with the command as the remainder" in {
    val result = argFinder.getArg("let the dogs out", emptyContext)

    assert(result == (Some(""), "let the dogs out"))
  }

  it should "return the command and no remainder for an exact match" in {
    val result = argFinder.getArg("tell", emptyContext)

    assert(result == (Some("tell"), ""))
  }

  it should "return the command and no remainder if the remainder is whitespace" in {
    val resultA = argFinder.getArg("tell ", emptyContext)
    val resultB = argFinder.getArg("tell  ", emptyContext)

    assert(resultA == (Some("tell"), ""))
    assert(resultB == (Some("tell"), ""))
  }

  it should "return the command and the lead trimmed remainder for a command match" in {
    val result = argFinder.getArg("tell  Kapunga Whatever! ", emptyContext)

    assert(result == (Some("tell"), "Kapunga Whatever! "))
  }
}
