package org.smallmind.nutsnbolts.swing.event;

import java.util.EventObject;

public class MultiListSelectionEvent extends EventObject {

   public static enum EventType {

      INSERT, REMOVE, CHANGE
   }

   ;

   private EventType eventType;
   private boolean valueIsAdjusting;
   private int firstIndex;
   private int lastIndex;

   public MultiListSelectionEvent (Object source, EventType eventType, int firstIndex, int lastIndex, boolean valueIsAdjusting) {

      super(source);

      this.eventType = eventType;
      this.firstIndex = firstIndex;
      this.lastIndex = lastIndex;
      this.valueIsAdjusting = valueIsAdjusting;
   }

   public boolean getValueIsAdjusting () {

      return valueIsAdjusting;
   }

   public EventType getEventType () {

      return eventType;
   }

   public int getFirstIndex () {

      return firstIndex;
   }

   public int getLastIndex () {

      return lastIndex;
   }

}
