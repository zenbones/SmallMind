package org.smallmind.nutsnbolts.reflection;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class ByteCodeManipulationException extends FormattedRuntimeException {

   public ByteCodeManipulationException () {

      super();
   }

   public ByteCodeManipulationException (String message, Object... args) {

      super(message, args);
   }

   public ByteCodeManipulationException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ByteCodeManipulationException (Throwable throwable) {

      super(throwable);
   }
}
