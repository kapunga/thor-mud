package org.kapunga.tm

import akka.actor.Actor
import org.kapunga.tm.soul.Soul
import org.mindrot.jbcrypt.BCrypt
import scala.io.Source.fromInputStream

/**
 * This actor handles login behavior.  It is relatively simple at the moment and behaves
 * like a finite state machine.  If a player does not already have a Soul, it creates one
 * for them, otherwise it checks a login.
 *
 * @author Paul J Thordarson kapunga@gmail.com
 */
class LoginHandler extends Actor {
  import context.become
  import context.parent

  // Print welcome and prompt user for name.
  printWelcome()
  parent ! Output("\nWhat's your name stranger?\n")
  parent ! Output("Please enter your name: ")

  var name = ""
  var password = ""
  var email = ""

  var attempts = 0
  var oldSoul: Option[Soul] = None


  def receive = {
    case Input(content) =>
      content match {
        case "quit" => quit()

        case input: String =>
          Soul.findByName(input) match {

            case Some(soul) =>
              oldSoul = Some(soul)
              parent ! Output(s"Welcome back ${soul.name}!\nEnter password: ")
              parent ! PasswordMode
              become(enterPassword, discardOld = true)

            case None =>
              name = input
              parent ! Output(s"You appear to be new here, $input.\nPlease confirm your name: ")
              become(confirmName, discardOld = true)
          }
      }
  }

  def enterPassword: Receive = {
    case Input(content) =>
      content match {
        case "quit" => quit()

        case input: String =>
          oldSoul match {
            case Some(soul) =>
              BCrypt.checkpw(input, soul.password) match {
                case true =>
                  parent ! Output(s"Welcome back ${soul.name}!\n")
                  parent ! LoggedIn(soul)
                case false =>
                  attempts += 1
                  if (attempts < LoginHandler.MAX_ATTEMPTS) {
                    parent ! PasswordMode
                    parent ! Output("Incorrect password.  Please enter again.\nEnter password: ")
                  } else {
                    parent ! Output("Exceeded max password attempts.\nPlease enter your name: ")
                    attempts = 0
                    become(receive, discardOld = true)
                  }
              }
            case None =>
              parent ! Output("There was a problem processing your password!\nPlease enter your name: ")
              become(receive, discardOld = true)
          }

      }
  }

  def confirmName: Receive = {
    case Input(content) =>
      content match {
        case "quit" => quit()
        case input: String =>
          if (name == input) {
            parent ! Output(s"$name, welcome!\nPlease choose a password: ")
            parent ! PasswordMode
            become(getPassword, discardOld = true)
          } else {
            parent ! Output(s"$input doesn't match $name.\nPlease enter name: ")
            become(receive, discardOld = true)
          }
      }
  }

  def getPassword: Receive = {
    case Input(content) =>
      content match {
        case "quit" => quit()
        case input: String =>
          password = BCrypt.hashpw(input, BCrypt.gensalt())
          parent ! Output(s"Great! Please enter it again to confirm.\nConfirm password: ")
          parent ! PasswordMode
          become(confirmPassword, discardOld = true)
      }
  }

  def confirmPassword: Receive = {
    case Input(content) =>
      content match {
        case "quit" => quit()
        case input: String =>
          BCrypt.checkpw(input, password) match {
            case true =>
              parent ! Output(s"Perfect!  Please provide an email address.\nEnter email: ")
              become(getEmail, discardOld = true)
            case false =>
              parent ! Output(s"Passwords don't match.\nPlease choose a password: ")
              parent ! PasswordMode
              become(getPassword, discardOld = true)
          }
      }
  }

  def getEmail: Receive = {
    case Input(content) =>
      content match {
        case "quit" => quit()
        case input: String =>
          email = input
          parent ! Output(s"Please confirm your email.\nConfirm email: ")
          become(confirmEmail, discardOld = true)
      }
  }

  def confirmEmail: Receive = {
    case Input(content) =>
      content match {
        case "quit" => quit()
        case input: String =>
          if (email == input) {
            Soul.createSoul(name, password, email) match {
              case Some(soul) =>
                parent ! Output(s"All set!  Welcome!\n")
                parent ! LoggedIn(soul)
              case None =>
                parent ! Output("Error creating soul!\nTry again later.")
                quit()
            }
          } else {
            parent ! Output(s"$input doesn't match $email!\nEnter email: ")
            become(getEmail, discardOld = true)
          }
      }
  }

  /*
   * Outputs a welcome screen.  This is stored in a text file.
   */
  private def printWelcome() = {
    val lines = fromInputStream(getClass.getResourceAsStream("/welcome.txt")).getLines

    lines.foreach(line => parent ! Output(line + "\n"))
  }

  /*
   * Sends a quit message to the parent.
   */
  private def quit() = parent ! CloseConnection("Bye Bye!\n")
}

/**
 * A companion object to the LoginHandler.  Currently just contains constants.
 */
object LoginHandler {
  /**
   * The maximum number of login attempts a user can make before being forced to start over.
   */
  val MAX_ATTEMPTS = 3
}
