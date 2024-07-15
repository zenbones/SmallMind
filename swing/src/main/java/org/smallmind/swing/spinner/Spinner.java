/*
 * Copyright (c) 2007 through 2024 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.spinner;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.swing.EditorEvent;
import org.smallmind.swing.EditorListener;
import org.smallmind.swing.button.ButtonRepeater;

public class Spinner extends JPanel implements EditorListener, ActionListener, ChangeListener, MouseListener, FocusListener {

  private static final ImageIcon SPINNER_UP = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/arrow_right_blue_16.png"));
  private static final ImageIcon SPINNER_DOWN = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/arrow_left_blue_16.png"));

  private final WeakEventListenerList<ChangeListener> listenerList;
  private final SpinnerModel spinnerModel;
  private final SpinnerRubberStamp rubberStamp;
  private final JPanel valuePanel;
  private final JButton spinnerUpButton;
  private final JButton spinnerDownButton;
  private final ButtonRepeater spinnerUpButtonRepeater;
  private final ButtonRepeater spinnerDownButtonRepeater;
  private SpinnerRenderer renderer;
  private SpinnerEditor editor;
  private boolean editing = false;

  public Spinner (SpinnerModel spinnerModel, long delayMilliseconds) {

    super();

    GroupLayout groupLayout;
    int valuePanelHeight;

    setLayout(groupLayout = new GroupLayout(this));

    this.spinnerModel = spinnerModel;

    rubberStamp = new SpinnerRubberStamp(this);
    setSpinnerRenderer(new DefaultSpinnerRenderer());

    spinnerUpButton = new JButton(SPINNER_UP);
    spinnerUpButton.setFocusable(false);
    spinnerUpButtonRepeater = new ButtonRepeater(spinnerUpButton, delayMilliseconds);

    spinnerDownButton = new JButton(SPINNER_DOWN);
    spinnerDownButton.setFocusable(false);
    spinnerDownButtonRepeater = new ButtonRepeater(spinnerDownButton, delayMilliseconds);

    valuePanel = new JPanel(new GridLayout(1, 1));
    valuePanel.add(rubberStamp);

    listenerList = new WeakEventListenerList<ChangeListener>();

    spinnerUpButtonRepeater.addActionListener(this);
    spinnerDownButtonRepeater.addActionListener(this);
    rubberStamp.addMouseListener(this);
    spinnerModel.addChangeListener(this);

    setEnabled(true);

    valuePanelHeight = (int)valuePanel.getPreferredSize().getHeight();

    groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup().addComponent(spinnerDownButton, 20, 20, 20).addComponent(valuePanel).addComponent(spinnerUpButton, 20, 20, 20));
    groupLayout.setVerticalGroup(groupLayout.createParallelGroup().addComponent(spinnerDownButton, valuePanelHeight, valuePanelHeight, valuePanelHeight).addComponent(valuePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(spinnerUpButton, valuePanelHeight, valuePanelHeight, valuePanelHeight));
  }

  public synchronized void addChangeListener (ChangeListener changeListener) {

    listenerList.addListener(changeListener);
  }

  public synchronized void removeChangeListener (ChangeListener changeListener) {

    listenerList.removeListener(changeListener);
  }

  public synchronized SpinnerRenderer getSpinnerRenderer () {

    return renderer;
  }

  public synchronized void setSpinnerRenderer (SpinnerRenderer renderer) {

    this.renderer = renderer;
    rubberStamp.repaint();
  }

  public synchronized SpinnerEditor getSpinnerEditor () {

    return editor;
  }

  public synchronized void setSpinnerEditor (SpinnerEditor editor) {

    if (this.editor != null) {
      this.editor.removeEditorListener(this);
    }

    this.editor = editor;
    editor.addEditorListener(this);
  }

  public synchronized Object getValue () {

    if (editing) {
      return (editor.isValid()) ? editor.getValue() : null;
    }

    return spinnerModel.getValue();
  }

  public synchronized void setValue (Object value) {

    spinnerModel.setValue(value);
  }

  public synchronized Component getRenderComponent () {

    return renderer.getSpinnerRendererComponent(this, getValue());
  }

  @Override
  public void setEnabled (boolean enabled) {

    if (enabled) {
      if (spinnerModel instanceof EdgeAwareSpinnerModel) {
        spinnerUpButton.setEnabled(!getValue().equals(((EdgeAwareSpinnerModel)spinnerModel).getMaximumValue()));
        spinnerDownButton.setEnabled(!getValue().equals(((EdgeAwareSpinnerModel)spinnerModel).getMinimumValue()));
      } else {
        spinnerUpButton.setEnabled(true);
        spinnerDownButton.setEnabled(true);
      }

      rubberStamp.repaint();
    } else {
      cancelEditing(false);
      rubberStamp.repaint();

      spinnerUpButton.setEnabled(false);
      spinnerDownButton.setEnabled(false);
    }

    super.setEnabled(enabled);
  }

  public void cancelEditing () {

    cancelEditing(true);
  }

  public synchronized void cancelEditing (boolean andEnable) {

    if (editing) {
      editing = false;

      valuePanel.getComponent(0).removeFocusListener(this);
      valuePanel.removeAll();
      valuePanel.add(rubberStamp);
      valuePanel.revalidate();

      if (andEnable) {
        setEnabled(true);
      }
    }
  }

  public synchronized void stopEditing () {

    if (editing) {
      spinnerModel.setValue(editor.getValue());

      cancelEditing();
    }
  }

  public synchronized void editorStatus (EditorEvent editorEvent) {

    switch (editorEvent.getState()) {
      case STOPPED:
        if (editor.isValid()) {
          stopEditing();
        } else {
          cancelEditing();
        }

        rubberStamp.repaint();
        break;
      case CANCELLED:
        cancelEditing();
        rubberStamp.repaint();
        break;
      case VALID:
        break;
      case INVALID:
        break;
      default:
        throw new UnknownSwitchCaseException(editorEvent.getState().name());
    }
  }

  public synchronized void actionPerformed (ActionEvent actionEvent) {

    if (editing) {
      stopEditing();
    }

    if (actionEvent.getSource() == spinnerUpButton) {
      spinnerModel.setValue(spinnerModel.getNextValue());
    } else if (actionEvent.getSource() == spinnerDownButton) {
      spinnerModel.setValue(spinnerModel.getPreviousValue());
    }
  }

  public synchronized void stateChanged (ChangeEvent changeEvent) {

    ChangeEvent spinnerChangeEvent;

    rubberStamp.repaint();

    if (spinnerModel instanceof EdgeAwareSpinnerModel) {
      spinnerUpButton.setEnabled(!spinnerModel.getValue().equals(((EdgeAwareSpinnerModel)spinnerModel).getMaximumValue()));
      spinnerDownButton.setEnabled(!spinnerModel.getValue().equals(((EdgeAwareSpinnerModel)spinnerModel).getMinimumValue()));
    }

    spinnerChangeEvent = new ChangeEvent(this);
    for (ChangeListener changeListener : listenerList) {
      changeListener.stateChanged(spinnerChangeEvent);
    }
  }

  public void mouseEntered (MouseEvent mouseEvent) {

  }

  public void mouseExited (MouseEvent mouseEvent) {

  }

  public void mouseClicked (MouseEvent mouseEvent) {

  }

  public void mouseReleased (MouseEvent mouseEvent) {

  }

  public synchronized void mousePressed (MouseEvent mouseEvent) {

    Component editorComponent;

    if (isEnabled() && (editor != null) && (!editing)) {
      editorComponent = editor.getSpinnerEditorComponent(this, getValue());
      editorComponent.setPreferredSize(rubberStamp.getPreferredSize());
      editorComponent.addFocusListener(this);

      valuePanel.removeAll();
      valuePanel.add(editorComponent);
      valuePanel.revalidate();

      editorComponent.requestFocusInWindow();
      editor.startEditing();
      editing = true;
    }
  }

  public synchronized void focusGained (FocusEvent focusEvent) {

  }

  public synchronized void focusLost (FocusEvent focusEvent) {

    cancelEditing();
  }
}
