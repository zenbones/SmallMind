package org.smallmind.swing.event;

import java.util.EventListener;

public interface EditorListener extends EventListener {

   public abstract void editorStatus (EditorEvent editorEvent);

}
