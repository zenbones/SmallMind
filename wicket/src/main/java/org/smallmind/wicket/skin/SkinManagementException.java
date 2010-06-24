package org.smallmind.wicket.skin;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class SkinManagementException extends FormattedRuntimeException {

   public SkinManagementException () {

      super();
   }

   public SkinManagementException (String message, Object... args) {

      super(message, args);
   }

   public SkinManagementException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public SkinManagementException (Throwable throwable) {

      super(throwable);
   }
}