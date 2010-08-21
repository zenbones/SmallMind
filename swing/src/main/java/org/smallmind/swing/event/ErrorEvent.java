package org.smallmind.swing.event;

public class ErrorEvent extends java.util.EventObject {

   private Exception e;

   public ErrorEvent (Object source, Exception e) {

      super(source);
      this.e = e;
   }

   public Exception getException () {

      return e;
   }

}





