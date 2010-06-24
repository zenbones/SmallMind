package org.smallmind.scribe.pen;

public class ConsoleAppender extends AbstractAppender {

   public ConsoleAppender () {

      this(null, null);
   }

   public ConsoleAppender (Formatter formatter) {

      this(formatter, null);
   }

   public ConsoleAppender (Formatter formatter, ErrorHandler errorHandler) {

      super(formatter, errorHandler);
   }

   public boolean requiresFormatter () {

      return true;
   }

   public synchronized void handleOutput (String formattedOutput) {

      System.out.print(formattedOutput);
   }
}