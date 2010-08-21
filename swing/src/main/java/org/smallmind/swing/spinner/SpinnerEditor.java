package org.smallmind.swing.spinner;

import java.awt.Component;
import org.smallmind.swing.event.EditorListener;

public interface SpinnerEditor {

   public abstract void addEditorListener (EditorListener editorListener);

   public abstract void removeEditorListener (EditorListener editorListener);

   public abstract boolean isValid ();

   public abstract Object getValue ();

   public abstract void startEditing ();

   public abstract Component getSpinnerEditorComponent (Spinner spinner, Object value);

}
