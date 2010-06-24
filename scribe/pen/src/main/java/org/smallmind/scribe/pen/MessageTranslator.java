package org.smallmind.scribe.pen;

import java.util.Arrays;
import java.util.IllegalFormatException;

public class MessageTranslator {

   public static String translateMessage (String message, Object... args) {

      if (message == null) {
         if (args.length > 0) {

            StringBuilder errorBuilder = new StringBuilder();

            errorBuilder.append("A null format can't apply to arguments ");
            errorBuilder.append(Arrays.toString(args));

            throw new MessageFormattingException(errorBuilder.toString());
         }

         return null;
      }
      else if (args.length == 0) {
         return message;
      }
      else {
         try {

            return String.format(message, args);
         }
         catch (IllegalFormatException illegalFormatException) {

            StringBuilder errorBuilder = new StringBuilder();

            errorBuilder.append("Error applying format (");
            errorBuilder.append(message);
            errorBuilder.append(") to arguments ");
            errorBuilder.append(Arrays.toString(args));

            throw new MessageFormattingException(illegalFormatException, errorBuilder.toString());
         }
      }
   }
}
