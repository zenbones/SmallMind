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
package org.smallmind.swing.text;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.swing.ComponentUtilities;

public class ExpandingTextField extends JPanel implements ActionListener, DocumentListener {

  private static final ImageIcon COLLAPSE_ICON = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("org/smallmind/swing/system/navigate_open_16.png"));
  private static final ImageIcon EXPAND_ICON = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("org/smallmind/swing/system/navigate_close_16.png"));

  private static enum CardState {COLLAPSED, EXPANDED}

  private final WeakEventListenerList<DocumentListener> listenerList = new WeakEventListenerList<DocumentListener>();

  private CardLayout cardLayout;
  private CardState cardState;
  private JScrollPane expandedScrollPane;
  private JTextArea expandedTextArea;
  private JTextField contractedTextField;
  private AtomicBoolean documentSensitive = new AtomicBoolean(true);

  public ExpandingTextField () {

    this(null);
  }

  public ExpandingTextField (String text) {

    this(text, 5);
  }

  public ExpandingTextField (int rows) {

    this(null, rows);
  }

  public ExpandingTextField (String text, int rows) {

    JPanel contractedPanel;
    JPanel expandedPanel;
    GroupLayout contractedGroupLayout;
    GroupLayout expandedGroupLayout;
    JButton collapseButton;
    JButton expandButton;
    int textFieldHeight;

    setLayout(cardLayout = new CardLayout());

    contractedPanel = new JPanel();
    contractedPanel.setLayout(contractedGroupLayout = new GroupLayout(contractedPanel));

    expandedPanel = new JPanel();
    expandedPanel.setLayout(expandedGroupLayout = new GroupLayout(expandedPanel));

    collapseButton = new JButton(COLLAPSE_ICON);
    collapseButton.setFocusable(false);
    collapseButton.addActionListener(this);

    expandButton = new JButton(EXPAND_ICON);
    expandButton.setFocusable(false);
    expandButton.addActionListener(this);

    contractedTextField = new JTextField(text);
    contractedTextField.getDocument().addDocumentListener(this);

    expandedScrollPane = new JScrollPane(expandedTextArea = new JTextArea(text, rows, 0), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    expandedTextArea.getDocument().addDocumentListener(this);

    textFieldHeight = (int)contractedTextField.getPreferredSize().getHeight();

    contractedGroupLayout.setHorizontalGroup(contractedGroupLayout.createSequentialGroup().addComponent(contractedTextField).addComponent(expandButton, 22, 22, 22));
    contractedGroupLayout.setVerticalGroup(contractedGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(contractedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(expandButton, textFieldHeight, textFieldHeight, textFieldHeight));

    expandedGroupLayout.setHorizontalGroup(expandedGroupLayout.createSequentialGroup().addComponent(expandedScrollPane).addComponent(collapseButton, 22, 22, 22));
    expandedGroupLayout.setVerticalGroup(expandedGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(expandedScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(collapseButton, textFieldHeight, textFieldHeight, textFieldHeight));

    add(contractedPanel, "field");
    add(expandedPanel, "area");

    setMinimumSize(new Dimension(ComponentUtilities.getMinimumWidth(expandedScrollPane), ComponentUtilities.getMinimumHeight(contractedTextField)));
    setPreferredSize(new Dimension(ComponentUtilities.getPreferredWidth(expandedScrollPane), ComponentUtilities.getPreferredHeight(contractedTextField)));
    setMaximumSize(new Dimension(ComponentUtilities.getMaximumWidth(expandedScrollPane), ComponentUtilities.getMaximumHeight(contractedTextField)));

    cardState = CardState.COLLAPSED;
  }

  @Override
  public void setEnabled (boolean enabled) {

    contractedTextField.setEnabled(enabled);
    expandedTextArea.setEnabled(enabled);

    super.setEnabled(enabled);
  }

  public String getText () {

    return expandedTextArea.getText();
  }

  public boolean containsDocument (Document document) {

    return (contractedTextField.getDocument() == document) || (expandedTextArea.getDocument() == document);
  }

  public void setText (String text) {

    switch (cardState) {
      case COLLAPSED:
        contractedTextField.setText(text);
        break;
      case EXPANDED:
        expandedTextArea.setText(text);
        break;
      default:
        throw new UnknownSwitchCaseException(cardState.name());
    }
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
        setMinimumSize(new Dimension(ComponentUtilities.getMinimumWidth(expandedScrollPane), ComponentUtilities.getMinimumHeight(contractedTextField)));
        setPreferredSize(new Dimension(ComponentUtilities.getPreferredWidth(expandedScrollPane), ComponentUtilities.getPreferredHeight(contractedTextField)));
        setMaximumSize(new Dimension(ComponentUtilities.getMaximumWidth(expandedScrollPane), ComponentUtilities.getMaximumHeight(contractedTextField)));

        cardState = CardState.COLLAPSED;
        break;
      default:
        throw new UnknownSwitchCaseException(cardState.name());
    }

    doLayout();
  }

  @Override
  public synchronized void insertUpdate (DocumentEvent documentEvent) {

    if (documentSensitive.get()) {
      documentSensitive.set(false);

      try {
        switch (cardState) {
          case COLLAPSED:
            expandedTextArea.getDocument().insertString(documentEvent.getOffset(), contractedTextField.getDocument().getText(documentEvent.getOffset(), documentEvent.getLength()), null);
            break;
          case EXPANDED:
            contractedTextField.getDocument().insertString(documentEvent.getOffset(), expandedTextArea.getDocument().getText(documentEvent.getOffset(), documentEvent.getLength()), null);
            break;
          default:
            throw new UnknownSwitchCaseException(cardState.name());
        }
      }
      catch (BadLocationException badLocationException) {
        throw new RuntimeException(badLocationException);
      }

      for (DocumentListener documentListener : listenerList) {
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
            contractedTextField.getDocument().remove(documentEvent.getOffset(), documentEvent.getLength());
            break;
          default:
            throw new UnknownSwitchCaseException(cardState.name());
        }
      }
      catch (BadLocationException badLocationException) {
        throw new RuntimeException(badLocationException);
      }

      for (DocumentListener documentListener : listenerList) {
        documentListener.removeUpdate(documentEvent);
      }

      documentSensitive.set(true);
    }
  }

  @Override
  public synchronized void changedUpdate (DocumentEvent documentEvent) {

    if (documentSensitive.get()) {
      documentSensitive.set(false);

      for (DocumentListener documentListener : listenerList) {
        documentListener.changedUpdate(documentEvent);
      }

      documentSensitive.set(true);
    }
  }

  public synchronized void addDocumentListener (DocumentListener documentListener) {

    listenerList.addListener(documentListener);
  }

  public synchronized void removeDocumentListener (DocumentListener documentListener) {

    listenerList.removeListener(documentListener);
  }
}