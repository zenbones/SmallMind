package org.smallmind.nutsnbolts.io;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class FileProcessingException extends FormattedException {

   public FileProcessingException () {

      super();
   }

   public FileProcessingException (String message, Object... args) {

      super(message, args);
   }

   public FileProcessingException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public FileProcessingException (Throwable throwable) {

      super(throwable);
   }
}
