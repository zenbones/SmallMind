package org.smallmind.nutsnbolts.xml;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class ExtensibleSAXParserException extends FormattedException {

   public ExtensibleSAXParserException () {

      super();
   }

   public ExtensibleSAXParserException (String message, Object... args) {

      super(message, args);
   }

   public ExtensibleSAXParserException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ExtensibleSAXParserException (Throwable throwable) {

      super(throwable);
   }
}

