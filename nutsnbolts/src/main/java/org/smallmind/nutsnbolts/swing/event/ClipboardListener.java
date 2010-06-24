package org.smallmind.nutsnbolts.swing.event;

import java.util.EventListener;

public interface ClipboardListener extends EventListener {

   public abstract void cutAction (ClipboardEvent clipboardEvent);

   public abstract void copyAction (ClipboardEvent clipboardEvent);

   public abstract void pasteAction (ClipboardEvent clipboardEvent);
}
