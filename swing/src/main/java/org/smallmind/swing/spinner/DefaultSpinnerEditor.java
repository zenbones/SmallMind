package org.smallmind.swing.spinner;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.smallmind.swing.event.EditorEvent;

public class DefaultSpinnerEditor extends AbstractSpinnerEditor implements DocumentListener {

   private JTextField editorField;

   public DefaultSpinnerEditor () {

      super();

      editorField = new JTextField();
      editorField.setFont(editorField.getFont().deriveFont(Font.BOLD));
      editorField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createMatteBorder(2, 2, 2, 2, SystemColor.text)));

      editorField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "stopEditing");
      editorField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelEditing");
      editorField.getActionMap().put("stopEditing", getStopEditingAction());
      editorField.getActionMap().put("cancelEditing", getCancelEditingAction());

      editorField.getDocument().addDocumentListener(this);
   }

   public boolean isValid () {

      return true;
   }

   public synchronized Object getValue () {

      return editorField.getText();
   }

   public void startEditing () {

      editorField.setCaretPosition(editorField.getText().length());
   }

   public synchronized Component getSpinnerEditorComponent (Spinner spinner, Object value) {

      editorField.setText(value.toString());

      return editorField;
   }

   private synchronized void checkValidty () {

      if (!isValid()) {
         editorField.setForeground(Color.RED);
         fireEditorStatus(new EditorEvent(this, EditorEvent.State.INVALID));
      }
      else {
         editorField.setForeground(SystemColor.textText);
         fireEditorStatus(new EditorEvent(this, EditorEvent.State.VALID));
      }
   }

   public synchronized void insertUpdate (DocumentEvent documentEvent) {

      checkValidty();
   }

   public synchronized void removeUpdate (DocumentEvent documentEvent) {

      checkValidty();
   }

   public synchronized void changedUpdate (DocumentEvent documentEvent) {

      checkValidty();
   }

}
