package org.smallmind.scribe.pen;

import java.util.IllegalFormatException;

public class MessageFormattingException extends RuntimeException {

   public MessageFormattingException (String message) {

      super(message);
   }

   public MessageFormattingException (IllegalFormatException illegalFormatException, String message) {

      super(message, illegalFormatException);
   }
}
