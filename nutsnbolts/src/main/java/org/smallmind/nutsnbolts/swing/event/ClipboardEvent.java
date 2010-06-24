package org.smallmind.nutsnbolts.swing.event;

import java.awt.Component;
import java.awt.datatransfer.Transferable;
import java.util.EventObject;

public final class ClipboardEvent extends EventObject {

   private Component target;
   private Transferable transferable;

   public ClipboardEvent (Object source, Component target) {

      this(source, target, null);
   }

   public ClipboardEvent (Object source, Component target, Transferable transferable) {

      super(source);
      this.target = target;
      this.transferable = transferable;
   }

   public Component getTargetComponent () {

      return target;
   }

   public Transferable getTransferable () {

      return transferable;
   }

}
