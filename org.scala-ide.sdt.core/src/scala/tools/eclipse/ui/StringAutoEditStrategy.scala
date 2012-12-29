package scala.tools.eclipse.ui

import scala.tools.eclipse.properties.EditorPreferencePage

import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.{ DocumentCommand, IAutoEditStrategy, IDocument }

/**
 * Applies several auto edit actions if one adds ore removes a sign inside of
 * a string.
 */
class StringAutoEditStrategy(prefStore: IPreferenceStore) extends IAutoEditStrategy {

  def customizeDocumentCommand(document: IDocument, command: DocumentCommand) {

    val isAutoEscapeEnabled = prefStore.getBoolean(
        EditorPreferencePage.P_ENABLE_AUTO_ESCAPE_LITERALS)

    def ch(i: Int, c: Char) = {
      val o = command.offset + i
      o >= 0 && o < document.getLength && document.getChar(o) == c
    }

    def removeClosingLiteral() {
      if (ch(-1, '\\') && (ch(0, '"') || ch(0, '\\'))) {
        command.length = 2
        command.offset -= 1
      }
    }

    def jumpOverClosingLiteral() {
      command.text = ""
      command.caretOffset = command.offset + 1
    }

    def handleClosingLiteral() {
      if (ch(0, '"') && ch(-1, '"') && !ch(-2, '\\'))
        jumpOverClosingLiteral()
      else if (isAutoEscapeEnabled) {
        if (ch(-1, '\\')) {
          if (ch(-2, '\\'))
            command.text = "\\\""
        }
        else command.text = "\\\""
      }
    }

    def handleEscapeSign() {
      if (ch(-1, '\\')) {
        if (ch(-2, '\\'))
          command.text = "\\\\"
      }
      else command.text = "\\\\"
    }

    if (command.length > 1) {
      /*
       * The current auto edit strategy is not able to handle changes that added or
       * removed more than a single sign, thus further calculations are aborted to
       * avoid wrong behavior.
       * This is only a temporary solution - in the future the needed behavior
       * should be implemented.
       */
      return
    }

    command.text match {
      case "\\" if isAutoEscapeEnabled => handleEscapeSign()
      case "\""                        => handleClosingLiteral()
      case "" if isAutoEscapeEnabled   => removeClosingLiteral()
      case _                           =>
    }
  }

}
