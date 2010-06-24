package org.smallmind.nutsnbolts.util.csv;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class CSVParseException extends FormattedException {

   public CSVParseException () {

      super();
   }

   public CSVParseException (String message, Object... args) {

      super(message, args);
   }

   public CSVParseException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public CSVParseException (Throwable throwable) {

      super(throwable);
   }
}