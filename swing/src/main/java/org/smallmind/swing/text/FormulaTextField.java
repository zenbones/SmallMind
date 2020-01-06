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
package org.smallmind.swing.text;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.swing.ComponentUtility;

public class FormulaTextField extends JPanel implements ActionListener, ItemListener, DocumentListener {

  private static final ImageIcon COLLAPSE_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/navigate_open_16.png"));
  private static final ImageIcon EXPAND_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/navigate_close_16.png"));
  private static final ImageIcon FORMULA_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/text_formula_16.png"));
  private static final ImageIcon TEXT_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/text_16.png"));

  private static enum CardState {COLLAPSED, EXPANDED}

  private final WeakEventListenerList<ItemListener> itemListenerList = new WeakEventListenerList<ItemListener>();
  private final WeakEventListenerList<DocumentListener> documentListenerList = new WeakEventListenerList<DocumentListener>();

  private CardLayout cardLayout;
  private CardState cardState;
  private JScrollPane expandedScrollPane;
  private JTextArea expandedTextArea;
  private SlimTextField collapsedTextField;
  private JToggleButton expandedFormulaButton;
  private JToggleButton collapsedFormulaButton;
  private AtomicBoolean documentSensitive = new AtomicBoolean(true);

  public FormulaTextField () {

    this(null, false);
  }

  public FormulaTextField (boolean formula) {

    this(null, formula);
  }

  public FormulaTextField (String text) {

    this(text, 5, false);
  }

  public FormulaTextField (String text, boolean formula) {

    this(text, 5, formula);
  }

  public FormulaTextField (int rows) {

    this(null, rows, false);
  }

  public FormulaTextField (int rows, boolean formula) {

    this(null, rows, formula);
  }

  public FormulaTextField (String text, int rows) {

    this(text, rows, false);
  }

  public FormulaTextField (String text, int rows, boolean formula) {

    JPanel collapsedPanel;
    JPanel expandedPanel;
    GroupLayout collapsedGroupLayout;
    GroupLayout expandedGroupLayout;
    JButton collapseButton;
    JButton expandButton;
    int textFieldHeight;

    setLayout(cardLayout = new CardLayout());

    collapsedPanel = new JPanel();
    collapsedPanel.setLayout(collapsedGroupLayout = new GroupLayout(collapsedPanel));

    expandedPanel = new JPanel();
    expandedPanel.setLayout(expandedGroupLayout = new GroupLayout(expandedPanel));

    collapseButton = new JButton(COLLAPSE_ICON);
    collapseButton.setFocusable(false);
    collapseButton.addActionListener(this);

    expandButton = new JButton(EXPAND_ICON);
    expandButton.setFocusable(false);
    expandButton.addActionListener(this);

    collapsedFormulaButton = new JToggleButton(TEXT_ICON, formula);
    collapsedFormulaButton.setSelectedIcon(FORMULA_ICON);
    collapsedFormulaButton.setFocusable(false);
    collapsedFormulaButton.setToolTipText("evaluate as text/formula");
    collapsedFormulaButton.addItemListener(this);

    expandedFormulaButton = new JToggleButton(TEXT_ICON, formula);
    expandedFormulaButton.setSelectedIcon(FORMULA_ICON);
    expandedFormulaButton.setFocusable(false);
    expandedFormulaButton.setToolTipText("evaluate as text/formula");
    expandedFormulaButton.addItemListener(this);

    collapsedTextField = new SlimTextField(text);
    collapsedTextField.getDocument().addDocumentListener(this);

    expandedScrollPane = new JScrollPane(expandedTextArea = new JTextArea(text, rows, 0), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    expandedTextArea.getDocument().addDocumentListener(this);

    textFieldHeight = (int)collapsedTextField.getPreferredSize().getHeight() - 1;

    collapsedGroupLayout.setHorizontalGroup(collapsedGroupLayout.createSequentialGroup().addComponent(collapsedTextField).addComponent(expandButton, 22, 22, 22).addComponent(collapsedFormulaButton, 22, 22, 22));
    collapsedGroupLayout.setVerticalGroup(collapsedGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(collapsedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(expandButton, textFieldHeight, textFieldHeight, textFieldHeight).addComponent(collapsedFormulaButton, textFieldHeight, textFieldHeight, textFieldHeight));

    expandedGroupLayout.setHorizontalGroup(expandedGroupLayout.createSequentialGroup().addComponent(expandedScrollPane).addComponent(collapseButton, 22, 22, 22).addComponent(expandedFormulaButton, 22, 22, 22));
    expandedGroupLayout.setVerticalGroup(expandedGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(expandedScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(collapseButton, textFieldHeight, textFieldHeight, textFieldHeight).addComponent(expandedFormulaButton, textFieldHeight, textFieldHeight, textFieldHeight));

    add(collapsedPanel, "field");
    add(expandedPanel, "area");

    setMinimumSize(new Dimension(144, ComponentUtility.getMinimumHeight(collapsedTextField)));
    setPreferredSize(new Dimension(Math.min(ComponentUtility.getPreferredWidth(collapsedTextField) + 44, Short.MAX_VALUE), ComponentUtility.getPreferredHeight(collapsedTextField)));
    setMaximumSize(new Dimension(Short.MAX_VALUE, ComponentUtility.getMaximumHeight(collapsedTextField)));

    cardState = CardState.COLLAPSED;
  }

  @Override
  public void setEnabled (boolean enabled) {

    collapsedTextField.setEnabled(enabled);
    expandedTextArea.setEnabled(enabled);
    collapsedFormulaButton.setEnabled(enabled);
    expandedFormulaButton.setEnabled(enabled);

    super.setEnabled(enabled);
  }

  public String getText () {

    return expandedTextArea.getText();
  }

  public boolean containsDocument (Document document) {

    return (collapsedTextField.getDocument() == document) || (expandedTextArea.getDocument() == document);
  }

  public boolean containsFormulaButton (JToggleButton toggleButton) {

    return (toggleButton == collapsedFormulaButton) || (toggleButton == expandedFormulaButton);
  }

  public void setText (String text) {

    switch (cardState) {
      case COLLAPSED:
        collapsedTextField.setText(text);
        break;
      case EXPANDED:
        expandedTextArea.setText(text);
        break;
      default:
        throw new UnknownSwitchCaseException(cardState.name());
    }
  }

  public boolean isFormula () {

    return expandedFormulaButton.isSelected();
  }

  @Override
  public synchronized void actionPerformed (ActionEvent actionEvent) {

    cardLayout.next(this);

    switch (cardState) {
      case COLLAPSED:
        setMinimumSize(expandedScrollPane.getMinimumSize());
        setPreferredSize(expandedScrollPane.getPreferredSize());
        setMaximumSize(expandedScrollPane.getMaximumSize());

        cardState = CardState.EXPANDED;
        break;
      case EXPANDED:
        setMinimumSize(new Dimension(ComponentUtility.getMinimumWidth(expandedScrollPane), ComponentUtility.getMinimumHeight(collapsedTextField)));
        setPreferredSize(new Dimension(ComponentUtility.getPreferredWidth(expandedScrollPane), ComponentUtility.getPreferredHeight(collapsedTextField)));
        setMaximumSize(new Dimension(ComponentUtility.getMaximumWidth(expandedScrollPane), ComponentUtility.getMaximumHeight(collapsedTextField)));

        cardState = CardState.COLLAPSED;
        break;
      default:
        throw new UnknownSwitchCaseException(cardState.name());
    }

    doLayout();
  }

  @Override
  public synchronized void itemStateChanged (ItemEvent itemEvent) {

    if (itemEvent.getSource() == collapsedFormulaButton) {
      expandedFormulaButton.setSelected(collapsedFormulaButton.isSelected());
    }
    else {
      collapsedFormulaButton.setSelected(expandedFormulaButton.isSelected());
    }

    for (ItemListener itemListener : itemListenerList) {
      itemListener.itemStateChanged(itemEvent);
    }
  }

  @Override
  public synchronized void insertUpdate (DocumentEvent documentEvent) {

    if (documentSensitive.get()) {
      documentSensitive.set(false);

      try {
        switch (cardState) {
          case COLLAPSED:
            expandedTextArea.getDocument().insertString(documentEvent.getOffset(), collapsedTextField.getDocument().getText(documentEvent.getOffset(), documentEvent.getLength()), null);
            break;
          case EXPANDED:
            collapsedTextField.getDocument().insertString(documentEvent.getOffset(), expandedTextArea.getDocument().getText(documentEvent.getOffset(), documentEvent.getLength()), null);
            break;
          default:
            throw new UnknownSwitchCaseException(cardState.name());
        }
      }
      catch (BadLocationException badLocationException) {
        throw new RuntimeException(badLocationException);
      }

      for (DocumentListener documentListener : documentListenerList) {
        documentListener.insertUpdate(documentEvent);
      }

      documentSensitive.set(true);
    }
  }

  @Override
  public synchronized void removeUpdate (DocumentEvent documentEvent) {

    if (documentSensitive.get()) {
      documentSensitive.set(false);

      try {
        switch (cardState) {
          case COLLAPSED:
            expandedTextArea.getDocument().remove(documentEvent.getOffset(), documentEvent.getLength());
            break;
          case EXPANDED:
            collapsedTextField.getDocument().remove(documentEvent.getOffset(), documentEvent.getLength());
            break;
          default:
            throw new UnknownSwitchCaseException(cardState.name());
        }
      }
      catch (BadLocationException badLocationException) {
        throw new RuntimeException(badLocationException);
      }

      for (DocumentListener documentListener : documentListenerList) {
        documentListener.removeUpdate(documentEvent);
      }

      documentSensitive.set(true);
    }
  }

  @Override
  public synchronized void changedUpdate (DocumentEvent documentEvent) {

    if (documentSensitive.get()) {
      documentSensitive.set(false);

      for (DocumentListener documentListener : documentListenerList) {
        documentListener.changedUpdate(documentEvent);
      }

      documentSensitive.set(true);
    }
  }

  public synchronized void addItemListener (ItemListener itemListener) {

    itemListenerList.addListener(itemListener);
  }

  public synchronized void removeItemListener (ItemListener itemListener) {

    itemListenerList.removeListener(itemListener);
  }

  public synchronized void addDocumentListener (DocumentListener documentListener) {

    documentListenerList.addListener(documentListener);
  }

  public synchronized void removeDocumentListener (DocumentListener documentListener) {

    documentListenerList.removeListener(documentListener);
  }
}