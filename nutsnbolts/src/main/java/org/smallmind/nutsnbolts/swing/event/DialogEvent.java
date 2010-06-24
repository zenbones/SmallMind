package org.smallmind.nutsnbolts.swing.event;

import org.smallmind.nutsnbolts.swing.dialog.DialogState;

public class DialogEvent extends java.util.EventObject {

   private DialogState eventState;

   public DialogEvent (Object source, DialogState eventState) {

      super(source);

      this.eventState = eventState;
   }

   public DialogState getEventState () {

      return eventState;
   }

}
