/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.swing.dialog;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class JavaErrorDialog extends JDialog implements ActionListener, WindowListener {

  private static final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();
  private static final ImageIcon BUG_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/dialog/dialog_bug.png"));

  private static final int DIALOG_WIDTH = 600;
  private static final int DIALOG_HEIGHT = 300;

  private final WeakEventListenerList<ErrorListener> listenerList;
  private final Object source;
  private final Exception exception;

  public JavaErrorDialog (Window parentWindow, Object source, Exception exception) {

    super(parentWindow, "Java Error Message...");

    StringWriter errorBuffer;
    PrintWriter errorWriter;
    GridBagConstraints constraint;
    Container contentPane;
    JPanel dialogPanel;
    JPanel workPanel;
    JLabel exceptionIconLabel;
    JScrollPane warningScroll;
    JTextArea exceptionTextArea;
    JButton continueButton;
    String exceptionText;

    this.source = source;
    this.exception = exception;

    listenerList = new WeakEventListenerList<ErrorListener>();
    addWindowListener(this);

    continueButton = new JButton("Continue");
    continueButton.registerKeyboardAction(this, "Continue", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), JComponent.WHEN_IN_FOCUSED_WINDOW);
    continueButton.addActionListener(this);

    setSize(DIALOG_WIDTH, DIALOG_HEIGHT);

    exceptionIconLabel = new JLabel(BUG_ICON);

    errorBuffer = new StringWriter();
    errorWriter = new PrintWriter(errorBuffer);
    exception.printStackTrace(errorWriter);
    exceptionText = errorBuffer.getBuffer().toString();
    errorWriter.close();

    exceptionTextArea = new JTextArea(exceptionText, 30, 75);
    exceptionTextArea.setEditable(false);
    exceptionTextArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 0, 0));

    warningScroll = new JScrollPane(exceptionTextArea);
    warningScroll.setBorder(BorderFactory.createLoweredBevelBorder());

    dialogPanel = new JPanel(GRID_BAG_LAYOUT);
    workPanel = new JPanel(GRID_BAG_LAYOUT);

    constraint = new GridBagConstraints();

    constraint.anchor = GridBagConstraints.NORTH;
    constraint.fill = GridBagConstraints.BOTH;
    constraint.insets = new Insets(0, 0, 0, 0);
    constraint.gridx = 0;
    constraint.gridy = 0;
    constraint.weightx = 1;
    constraint.weighty = 1;
    workPanel.add(warningScroll, constraint);

    constraint.anchor = GridBagConstraints.EAST;
    constraint.fill = GridBagConstraints.NONE;
    constraint.insets = new Insets(15, 0, 0, 0);
    constraint.gridx = 0;
    constraint.gridy = 1;
    constraint.weightx = 0;
    constraint.weighty = 0;
    workPanel.add(continueButton, constraint);

    constraint.anchor = GridBagConstraints.NORTHWEST;
    constraint.fill = GridBagConstraints.NONE;
    constraint.insets = new Insets(5, 5, 5, 0);
    constraint.gridx = 0;
    constraint.gridy = 0;
    constraint.weightx = 0;
    constraint.weighty = 0;
    dialogPanel.add(exceptionIconLabel, constraint);

    constraint.anchor = GridBagConstraints.NORTHEAST;
    constraint.fill = GridBagConstraints.BOTH;
    constraint.insets = new Insets(5, 15, 5, 5);
    constraint.gridx = 1;
    constraint.gridy = 0;
    constraint.weightx = 1;
    constraint.weighty = 1;
    dialogPanel.add(workPanel, constraint);

    contentPane = getContentPane();
    contentPane.setLayout(new GridLayout(1, 0));
    contentPane.add(dialogPanel);
  }

  public static void showJavaErrorDialog (Window parentWindow, Object source, Exception exception) {

    JavaErrorDialog errorDialog = new JavaErrorDialog(parentWindow, source, exception);

    errorDialog.setModal(true);
    errorDialog.setLocationRelativeTo(parentWindow);
    errorDialog.setVisible(true);
  }

  public synchronized void addErrorListener (ErrorListener errorListener) {

    listenerList.addListener(errorListener);
  }

  public synchronized void removErrorListener (ErrorListener errorListener) {

    listenerList.removeListener(errorListener);
  }

  public void actionPerformed (ActionEvent a) {

    windowClosing(null);
  }

  public void windowOpened (WindowEvent w) {

  }

  public void windowClosing (WindowEvent w) {

    Iterator<ErrorListener> listenerIter = listenerList.getListeners();
    ErrorEvent errorEvent;

    setVisible(false);

    if (source != null) {
      errorEvent = new ErrorEvent(source, exception);
      while (listenerIter.hasNext()) {
        listenerIter.next().errorHandler(errorEvent);
      }
    }

    dispose();
  }

  public void windowClosed (WindowEvent w) {

  }

  public void windowIconified (WindowEvent w) {

  }

  public void windowDeiconified (WindowEvent w) {

  }

  public void windowActivated (WindowEvent w) {

  }

  public void windowDeactivated (WindowEvent w) {

  }
}
