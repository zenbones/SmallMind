/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
import org.smallmind.swing.EditorEvent;

public class DefaultSpinnerEditor extends AbstractSpinnerEditor implements DocumentListener {

  private JTextField editorField;

  public DefaultSpinnerEditor () {

    this(JTextField.LEFT);
  }

  public DefaultSpinnerEditor (int alignment) {

    super();

    editorField = new JTextField();
    editorField.setHorizontalAlignment(JTextField.RIGHT);
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
