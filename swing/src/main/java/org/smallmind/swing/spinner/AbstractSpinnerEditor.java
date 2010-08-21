package org.smallmind.swing.spinner;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.smallmind.swing.event.EditorEvent;
import org.smallmind.swing.event.EditorListener;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public abstract class AbstractSpinnerEditor implements SpinnerEditor {

   private WeakEventListenerList<EditorListener> listenerList;

   private StopEditingAction stopEditingAction;
   private CancelEditingAction cancelEditingAction;

   public AbstractSpinnerEditor () {

      stopEditingAction = new StopEditingAction(this);
      cancelEditingAction = new CancelEditingAction(this);

      listenerList = new WeakEventListenerList<EditorListener>();
   }

   public StopEditingAction getStopEditingAction () {

      return stopEditingAction;
   }

   public CancelEditingAction getCancelEditingAction () {

      return cancelEditingAction;
   }

   public synchronized void addEditorListener (EditorListener editorListener) {

      listenerList.addListener(editorListener);
   }

   public synchronized void removeEditorListener (EditorListener editorListener) {

      listenerList.removeListener(editorListener);
   }

   public synchronized void fireEditorStatus (EditorEvent editorEvent) {

      for (EditorListener editorListener : listenerList) {
         editorListener.editorStatus(editorEvent);
      }
   }

   public class StopEditingAction extends AbstractAction {

      AbstractSpinnerEditor editor;

      public StopEditingAction (AbstractSpinnerEditor editor) {

         this.editor = editor;
      }

      public void actionPerformed (ActionEvent actionEvent) {

         fireEditorStatus(new EditorEvent(editor, EditorEvent.State.STOPPED));
      }

   }

   public class CancelEditingAction extends AbstractAction {

      AbstractSpinnerEditor editor;

      public CancelEditingAction (AbstractSpinnerEditor editor) {

         this.editor = editor;
      }

      public void actionPerformed (ActionEvent actionEvent) {

         fireEditorStatus(new EditorEvent(editor, EditorEvent.State.CANCELLED));
      }

   }

}
