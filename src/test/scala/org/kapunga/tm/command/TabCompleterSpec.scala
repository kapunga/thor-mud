package org.kapunga.tm.command

import org.kapunga.tm.EmptyTab
import org.scalatest.FlatSpec

/**
 * Created by kapunga on 5/3/15.
 */
class TabCompleterSpec extends FlatSpec {
  class TabCompleterImpl(strings: Iterable[String]) extends TabCompleter {
    override def options = strings
  }

  val stringSet = Set("s", "say", "south", "whisper", "who")

  val emptyContext = new ExecContext(null, null)

  val impl = new TabCompleterImpl(stringSet)

  "A TabCompleter" should "return an empty tab complete with no matches." in {
    val result = impl.doComplete("z", emptyContext)

    assert(result.get == EmptyTab)
  }
}
