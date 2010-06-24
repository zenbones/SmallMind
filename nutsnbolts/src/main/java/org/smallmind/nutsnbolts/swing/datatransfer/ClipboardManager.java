package org.smallmind.nutsnbolts.swing.datatransfer;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashMap;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.smallmind.nutsnbolts.swing.event.ClipboardEvent;
import org.smallmind.nutsnbolts.swing.event.ClipboardListener;

public class ClipboardManager implements ClipboardOwner, FocusListener {

   private Clipboard systemClipboard;
   private Action cutAction;
   private Action copyAction;
   private Action pasteAction;
   private Component selectedComponent;
   private HashMap<Component, ClipboardListener> listenerMap;

   public ClipboardManager () {

      cutAction = new CutAction(this, "Cut");
      copyAction = new CopyAction(this, "Copy");
      pasteAction = new PasteAction(this, "Paste");

      systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      listenerMap = new HashMap<Component, ClipboardListener>();
   }

   public Action getCutAction () {

      return cutAction;
   }

   public Action getCopyAction () {

      return copyAction;
   }

   public Action getPasteAction () {

      return pasteAction;
   }

   public synchronized void addClipboardListener (ClipboardListener clipboardListener, Component clipboardUser) {

      clipboardUser.addFocusListener(this);
      listenerMap.put(clipboardUser, clipboardListener);
   }

   public synchronized void removeClipboardListener (ClipboardListener clipboardListener, Component clipboardUser) {

      clipboardUser.removeFocusListener(this);
      listenerMap.remove(clipboardUser);
   }

   public void focusGained (FocusEvent focusEvent) {

      selectedComponent = (Component)focusEvent.getSource();
   }

   public void focusLost (FocusEvent focusEvent) {

      selectedComponent = null;
   }

   public void setContents (Transferable transferable) {

      systemClipboard.setContents(transferable, this);
   }

   public void lostOwnership (Clipboard clipboard, Transferable contents) {
   }

   public class CutAction extends AbstractAction {

      private ClipboardManager clipboardManager;

      public CutAction (ClipboardManager clipboardManager, String name) {

         super(name);

         this.clipboardManager = clipboardManager;
      }

      public void actionPerformed (ActionEvent actionEvent) {

         ClipboardEvent clipboardEvent;

         if (selectedComponent != null) {
            (listenerMap.get(selectedComponent)).cutAction(new ClipboardEvent(clipboardManager, selectedComponent));
         }
      }

   }

   public class CopyAction extends AbstractAction {

      private ClipboardManager clipboardManager;

      public CopyAction (ClipboardManager clipboardManager, String name) {

         super(name);

         this.clipboardManager = clipboardManager;
      }

      public void actionPerformed (ActionEvent actionEvent) {

         if (selectedComponent != null) {
            (listenerMap.get(selectedComponent)).copyAction(new ClipboardEvent(clipboardManager, selectedComponent));
         }
      }

   }

   public class PasteAction extends AbstractAction {

      private ClipboardManager clipboardManager;

      public PasteAction (ClipboardManager clipboardManager, String name) {

         super(name);

         this.clipboardManager = clipboardManager;
      }

      public void actionPerformed (ActionEvent actionEvent) {

         Transferable transferable;

         transferable = systemClipboard.getContents(clipboardManager);
         if (selectedComponent != null) {
            (listenerMap.get(selectedComponent)).pasteAction(new ClipboardEvent(clipboardManager, selectedComponent, transferable));
         }
      }

   }

}
