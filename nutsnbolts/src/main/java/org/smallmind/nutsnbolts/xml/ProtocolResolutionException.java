package org.smallmind.nutsnbolts.xml;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class ProtocolResolutionException extends FormattedException {

   public ProtocolResolutionException () {

      super();
   }

   public ProtocolResolutionException (String message, Object... args) {

      super(message, args);
   }

   public ProtocolResolutionException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ProtocolResolutionException (Throwable throwable) {

      super(throwable);
   }
}
