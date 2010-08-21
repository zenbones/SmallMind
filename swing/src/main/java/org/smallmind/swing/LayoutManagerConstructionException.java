package org.smallmind.swing;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class LayoutManagerConstructionException extends FormattedException {

   public LayoutManagerConstructionException () {

      super();
   }

   public LayoutManagerConstructionException (String message, Object... args) {

      super(message, args);
   }

   public LayoutManagerConstructionException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public LayoutManagerConstructionException (Throwable throwable) {

      super(throwable);
   }
}

