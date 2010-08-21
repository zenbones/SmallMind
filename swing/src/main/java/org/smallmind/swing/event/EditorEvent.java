package org.smallmind.swing.event;

import java.util.EventObject;

public class EditorEvent extends EventObject {

   public static enum State {

      STOPPED, CANCELLED, VALID, INVALID
   }

   private State state;

   public EditorEvent (Object source, State state) {

      super(source);

      this.state = state;
   }

   public State getState () {

      return state;
   }

}
