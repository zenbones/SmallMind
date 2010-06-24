package org.smallmind.nutsnbolts.swing.event;

import java.util.EventListener;

public interface EditorListener extends EventListener {

   public abstract void editorStatus (EditorEvent editorEvent);

}
