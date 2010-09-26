/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.swing.ButtonRepeater;
import org.smallmind.swing.ComponentUtilities;
import org.smallmind.swing.LayoutManagerConstructionException;
import org.smallmind.swing.LayoutManagerFactory;
import org.smallmind.swing.event.EditorEvent;
import org.smallmind.swing.event.EditorListener;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class Spinner extends JPanel implements EditorListener, ActionListener, ChangeListener, MouseListener {

   private static ImageIcon SPINNER_UP;
   private static ImageIcon SPINNER_DOWN;

   private WeakEventListenerList<ChangeListener> listenerList;
   private SpinnerModel spinnerModel;
   private SpinnerRenderer renderer;
   private SpinnerEditor editor;
   private SpinnerRubberStamp rubberStamp;
   private JPanel valuePanel;
   private JButton spinnerUpButton;
   private JButton spinnerDownButton;
   private ButtonRepeater spinnerUpButtonRepeater;
   private ButtonRepeater spinnerDownButtonRepeater;
   private boolean editing = false;

   static {

      SPINNER_UP = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/arrow_right_blue.png"));
      SPINNER_DOWN = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/arrow_left_blue.png"));
   }

   public Spinner (SpinnerModel spinnerModel, long delayMilliseconds)
      throws LayoutManagerConstructionException {

      super(LayoutManagerFactory.getLayoutManager(GridBagLayout.class));

      GridBagConstraints constraint;

      this.spinnerModel = spinnerModel;

      rubberStamp = new SpinnerRubberStamp(this);

      setSpinnerRenderer(new DefaultSpinnerRenderer());

      spinnerUpButton = new JButton(SPINNER_UP);
      spinnerUpButton.setFocusable(false);
      ComponentUtilities.setPreferredWidth(spinnerUpButton, 18);
      ComponentUtilities.setMinimumWidth(spinnerUpButton, 18);
      ComponentUtilities.setMaximumWidth(spinnerUpButton, 18);

      spinnerUpButtonRepeater = new ButtonRepeater(spinnerUpButton, delayMilliseconds);

      spinnerDownButton = new JButton(SPINNER_DOWN);
      spinnerDownButton.setFocusable(false);
      ComponentUtilities.setPreferredWidth(spinnerDownButton, 18);
      ComponentUtilities.setMinimumWidth(spinnerDownButton, 18);
      ComponentUtilities.setMaximumWidth(spinnerDownButton, 18);

      spinnerDownButtonRepeater = new ButtonRepeater(spinnerDownButton, delayMilliseconds);

      valuePanel = new JPanel(LayoutManagerFactory.getLayoutManager(GridLayout.class, new Class[] {int.class, int.class}, new Object[] {1, 1}));
      valuePanel.add(rubberStamp);

      constraint = new GridBagConstraints();

      constraint.anchor = GridBagConstraints.WEST;
      constraint.fill = GridBagConstraints.VERTICAL;
      constraint.gridx = 0;
      constraint.gridy = 0;
      constraint.weightx = 0;
      constraint.weighty = 1;
      add(spinnerDownButton, constraint);

      constraint.anchor = GridBagConstraints.NORTH;
      constraint.fill = GridBagConstraints.BOTH;
      constraint.gridx = 1;
      constraint.gridy = 0;
      constraint.weightx = 1;
      constraint.weighty = 1;
      add(valuePanel, constraint);

      constraint.anchor = GridBagConstraints.EAST;
      constraint.fill = GridBagConstraints.VERTICAL;
      constraint.gridx = 2;
      constraint.gridy = 0;
      constraint.weightx = 0;
      constraint.weighty = 1;
      add(spinnerUpButton, constraint);

      listenerList = new WeakEventListenerList<ChangeListener>();

      spinnerUpButtonRepeater.addActionListener(this);
      spinnerDownButtonRepeater.addActionListener(this);
      rubberStamp.addMouseListener(this);
      spinnerModel.addChangeListener(this);
   }

   public synchronized void addChangeListener (ChangeListener changeListener) {

      listenerList.addListener(changeListener);
   }

   public synchronized void removeChangeListener (ChangeListener changeListener) {

      listenerList.removeListener(changeListener);
   }

   public synchronized SpinnerRenderer getSpinnerRednerer () {

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

   public synchronized void enableSpinning () {

      if (spinnerModel instanceof EdgeAwareSpinnerModel) {
         spinnerUpButton.setEnabled(!getValue().equals(((EdgeAwareSpinnerModel)spinnerModel).getMaximumValue()));
         spinnerDownButton.setEnabled(!getValue().equals(((EdgeAwareSpinnerModel)spinnerModel).getMinimumValue()));
      }
      else {
         spinnerUpButton.setEnabled(true);
         spinnerDownButton.setEnabled(true);
      }
   }

   public synchronized void disableSpinning () {

      spinnerUpButton.setEnabled(false);
      spinnerDownButton.setEnabled(false);
   }

   public synchronized void cancelEditing () {

      if (editing) {
         editing = false;

         valuePanel.removeAll();
         valuePanel.add(rubberStamp);
         valuePanel.revalidate();

         enableSpinning();
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
               rubberStamp.repaint();
            }
            break;
         case CANCELLED:
            cancelEditing();
            rubberStamp.repaint();
            break;
         case VALID:
            enableSpinning();
            break;
         case INVALID:
            disableSpinning();
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
      }
      else if (actionEvent.getSource() == spinnerDownButton) {
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

      if ((editor != null) && (!editing)) {
         editorComponent = editor.getSpinnerEditorComponent(this, getValue());
         editorComponent.setPreferredSize(rubberStamp.getPreferredSize());

         valuePanel.removeAll();
         valuePanel.add(editorComponent);
         valuePanel.revalidate();

         editorComponent.requestFocusInWindow();
         editor.startEditing();
         editing = true;
      }
   }

   public void finalize ()
      throws Throwable {

      spinnerUpButtonRepeater.finalize();
      spinnerDownButtonRepeater.finalize();

      super.finalize();
   }
}
