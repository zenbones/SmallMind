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
package org.smallmind.swing.command;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import org.smallmind.nutsnbolts.command.CommandSet;
import org.smallmind.nutsnbolts.command.template.CommandArguments;
import org.smallmind.nutsnbolts.command.template.CommandGroup;
import org.smallmind.nutsnbolts.command.template.CommandStructure;
import org.smallmind.nutsnbolts.command.template.CommandTemplate;
import org.smallmind.nutsnbolts.util.StringUtilities;
import org.smallmind.swing.ComponentUtilities;
import org.smallmind.swing.signal.IndicatorBall;
import org.smallmind.swing.signal.ReadySetGo;

public class CommandDialog extends JDialog implements CaretListener, ItemListener, WindowListener {

   private CommandSet outputCommandSet = null;
   private HashMap<JComponent, CommandGroup> groupMap;
   private HashMap<JComponent, String> nameMap;
   private HashMap<CommandGroup, IndicatorBall> indicatorMap;
   private RunAction runAction;

   public static CommandSet showCommand (CommandTemplate commandTemplate) {

      return showCommand(commandTemplate, null);
   }

   public static CommandSet showCommand (CommandTemplate commandTemplate, CommandSet inputCommandSet) {

      CommandDialog commandDialog;

      commandDialog = new CommandDialog(commandTemplate, inputCommandSet);
      commandDialog.setModal(true);
      commandDialog.setVisible(true);

      return commandDialog.getOutputCommandSet();
   }

   public static CommandSet showCommand (JFrame parentFrame, CommandTemplate commandTemplate) {

      return showCommand(parentFrame, commandTemplate, null);
   }

   public static CommandSet showCommand (JFrame parentFrame, CommandTemplate commandTemplate, CommandSet inputCommandSet) {

      CommandDialog commandDialog;

      commandDialog = new CommandDialog(parentFrame, commandTemplate, inputCommandSet);
      commandDialog.setModal(true);
      commandDialog.setVisible(true);

      return commandDialog.getOutputCommandSet();
   }

   public static CommandSet showCommand (JDialog parentDialog, CommandTemplate commandTemplate) {

      return showCommand(parentDialog, commandTemplate, null);
   }

   public static CommandSet showCommand (JDialog parentDialog, CommandTemplate commandTemplate, CommandSet inputCommandSet) {

      CommandDialog commandDialog;

      commandDialog = new CommandDialog(parentDialog, commandTemplate, inputCommandSet);
      commandDialog.setModal(true);
      commandDialog.setVisible(true);

      return commandDialog.getOutputCommandSet();
   }

   public CommandDialog (CommandTemplate commandTemplate) {

      this(commandTemplate, null);
   }

   public CommandDialog (CommandTemplate commandTemplate, CommandSet inputCommandSet) {

      super();

      setTitle(commandTemplate.getShortName() + "...");
      buildDialog(null, commandTemplate, inputCommandSet);
   }

   public CommandDialog (JFrame parentFrame, CommandTemplate commandTemplate) {

      this(parentFrame, commandTemplate, null);
   }

   public CommandDialog (JFrame parentFrame, CommandTemplate commandTemplate, CommandSet inputCommandSet) {

      super(parentFrame, commandTemplate.getShortName() + "...");

      buildDialog(parentFrame, commandTemplate, inputCommandSet);
   }

   public CommandDialog (JDialog parentDialog, CommandTemplate commandTemplate) {

      this(parentDialog, commandTemplate, null);
   }

   public CommandDialog (JDialog parentDialog, CommandTemplate commandTemplate, CommandSet inputCommandSet) {

      super(parentDialog, commandTemplate.getShortName() + "...");

      buildDialog(parentDialog, commandTemplate, inputCommandSet);
   }

   private void buildDialog (Window parentWindow, CommandTemplate commandTemplate, CommandSet inputCommandSet) {

      GridBagLayout gridBag = new GridBagLayout();
      GridBagConstraints constraints = new GridBagConstraints();
      Container contentPane;
      JPanel dialogPanel;
      JPanel selectionPanel;
      JPanel buttonPanel;
      JPanel groupPanel;
      JButton cancelButton;
      JButton runButton;
      JComboBox argumentComboBox;
      JTextField argumentTextField;
      JLabel commandLabel;
      IndicatorBall groupIndicatorBall;
      CommandGroup[] commandGroups;
      CommandStructure[] commandStructures;
      CommandArguments commandArgumments;
      HashMap<JLabel, String> commandMap;
      LinkedList<JLabel> commandLabelList;
      Iterator<JLabel> commandLabelIter;
      int maxLabelWidth = 0;
      int groupYPos;
      int selectionYPos = 0;

      groupMap = new HashMap<JComponent, CommandGroup>();
      nameMap = new HashMap<JComponent, String>();
      indicatorMap = new HashMap<CommandGroup, IndicatorBall>();
      runAction = new RunAction();

      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

      commandGroups = commandTemplate.getCommandGroups();
      dialogPanel = new JPanel(gridBag);
      selectionPanel = new JPanel(gridBag);
      selectionPanel.setBorder(BorderFactory.createEtchedBorder());
      buttonPanel = new JPanel(gridBag);

      commandLabelList = new LinkedList<JLabel>();
      commandMap = new HashMap<JLabel, String>();
      for (CommandGroup commandGroup : commandGroups) {
         commandStructures = commandGroup.getCommandStructures();
         for (CommandStructure commandStructure : commandStructures) {
            commandLabelList.add(new JLabel(StringUtilities.toDisplayCase(commandStructure.getName(), '_')));
            commandMap.put(commandLabelList.getLast(), commandStructure.getName());
            if ((commandLabelList.getLast()).getPreferredSize().getWidth() > maxLabelWidth) {
               maxLabelWidth = (int)(commandLabelList.getLast()).getPreferredSize().getWidth();
            }
         }
      }

      commandLabelIter = commandLabelList.iterator();
      for (int count = 0; count < commandGroups.length; count++) {
         if (commandGroups[count].isNominalGroup()) {
            groupPanel = new JPanel(gridBag);
         }
         else {
            groupPanel = new JPanel(gridBag);
            groupPanel.setBorder(BorderFactory.createEtchedBorder());
         }

         groupYPos = 0;
         commandStructures = commandGroups[count].getCommandStructures();

         for (int loop = 0; loop < commandStructures.length; loop++) {
            commandLabel = commandLabelIter.next();
            ComponentUtilities.setPreferredWidth(commandLabel, maxLabelWidth);
            commandArgumments = commandStructures[loop].getCommandArguments();

            constraints.gridx = 0;
            constraints.gridy = groupYPos;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets(((!commandGroups[count].isNominalGroup()) && (loop == 0)) ? 5 : 0, 5, 5, 0);
            constraints.weightx = 0;
            constraints.weighty = 0;
            groupPanel.add(commandLabel, constraints);

            constraints.gridx = 1;
            constraints.gridy = groupYPos++;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(((!commandGroups[count].isNominalGroup()) && (loop == 0)) ? 5 : 0, 10, 5, 5);
            constraints.weightx = 1;
            constraints.weighty = 0;
            if (commandArgumments.areUnrestricted()) {
               argumentTextField = new JTextField();
               ComponentUtilities.setPreferredWidth(argumentTextField, 125);

               if (inputCommandSet.containsCommand(commandMap.get(commandLabel))) {
                  argumentTextField.setText(inputCommandSet.getArgument(commandMap.get(commandLabel)));
               }

               argumentTextField.addCaretListener(this);
               groupMap.put(argumentTextField, commandGroups[count]);
               nameMap.put(argumentTextField, commandMap.get(commandLabel));
               groupPanel.add(argumentTextField, constraints);
            }
            else {
               argumentComboBox = new JComboBox(commandArgumments.getArguments());
               argumentComboBox.setEditable(false);

               if ((inputCommandSet != null) && inputCommandSet.containsCommand(commandMap.get(commandLabel))) {
                  argumentComboBox.setSelectedItem(inputCommandSet.getArgument(commandMap.get(commandLabel)));
               }

               argumentComboBox.addItemListener(this);
               groupMap.put(argumentComboBox, commandGroups[count]);
               nameMap.put(argumentComboBox, commandMap.get(commandLabel));
               groupPanel.add(argumentComboBox, constraints);
            }
         }

         constraints.gridx = 0;
         constraints.gridy = selectionYPos;
         constraints.anchor = GridBagConstraints.NORTHWEST;
         constraints.fill = GridBagConstraints.HORIZONTAL;
         constraints.insets = new Insets((count == 0) ? 5 : 0, 5, (commandGroups[count].isNominalGroup()) ? 0 : 5, 0);
         constraints.weightx = 1;
         constraints.weighty = 0;
         selectionPanel.add(groupPanel, constraints);

         constraints.gridx = 1;
         constraints.gridy = selectionYPos++;
         constraints.anchor = GridBagConstraints.NORTHWEST;
         constraints.fill = GridBagConstraints.HORIZONTAL;
         constraints.insets = new Insets((count == 0) ? 5 : 0, 5, 0, 5);
         constraints.weightx = 0;
         constraints.weighty = 0;
         groupIndicatorBall = new IndicatorBall(ReadySetGo.RED);
         indicatorMap.put(commandGroups[count], groupIndicatorBall);
         selectionPanel.add(groupIndicatorBall, constraints);
      }

      cancelButton = new JButton(new CancelAction());
      runButton = new JButton(runAction);

      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.anchor = GridBagConstraints.SOUTHEAST;
      constraints.fill = GridBagConstraints.NONE;
      constraints.insets = new Insets(0, 0, 0, 0);
      constraints.weightx = 1;
      constraints.weighty = 0;
      buttonPanel.add(cancelButton, constraints);

      constraints.gridx = 1;
      constraints.gridy = 0;
      constraints.anchor = GridBagConstraints.SOUTHEAST;
      constraints.fill = GridBagConstraints.NONE;
      constraints.insets = new Insets(0, 5, 0, 0);
      constraints.weightx = 0;
      constraints.weighty = 0;
      buttonPanel.add(runButton, constraints);

      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.anchor = GridBagConstraints.NORTHEAST;
      constraints.fill = GridBagConstraints.BOTH;
      constraints.insets = new Insets(5, 5, 0, 5);
      constraints.weightx = 1;
      constraints.weighty = 1;
      dialogPanel.add(selectionPanel, constraints);

      constraints.gridx = 0;
      constraints.gridy = 1;
      constraints.anchor = GridBagConstraints.SOUTHWEST;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.insets = new Insets(10, 5, 5, 5);
      constraints.weightx = 0;
      constraints.weighty = 0;
      dialogPanel.add(buttonPanel, constraints);

      contentPane = getContentPane();
      contentPane.setLayout(new GridLayout(1, 0));
      contentPane.add(dialogPanel);

      for (CommandGroup commandGroup : commandGroups) {
         setGroupIndicator(commandGroup);
      }

      pack();
      setResizable(false);
      setLocationRelativeTo(parentWindow);

      addWindowListener(this);
   }

   public synchronized CommandSet getOutputCommandSet () {

      return outputCommandSet;
   }

   private String getArgument (Component commandComponent) {

      if (commandComponent instanceof JTextField) {
         return ((JTextField)commandComponent).getText();
      }
      else if (commandComponent instanceof JComboBox) {
         return (String)((JComboBox)commandComponent).getSelectedItem();
      }

      return null;
   }

   private boolean isFilled (Component commandComponent) {

      return (!getArgument(commandComponent).equals(""));
   }

   private void setGroupIndicator (CommandGroup commandGroup) {

      Iterator componentIter;
      Iterator<CommandGroup> groupIter;
      Component commandComponent;
      IndicatorBall groupIndicatorBall;
      boolean runnable = true;
      int filledCount = 0;

      componentIter = groupMap.keySet().iterator();
      while (componentIter.hasNext()) {
         commandComponent = (Component)componentIter.next();
         if (groupMap.get(commandComponent).equals(commandGroup)) {
            if (isFilled(commandComponent)) {
               filledCount++;
            }
         }
      }

      groupIndicatorBall = indicatorMap.get(commandGroup);
      if (filledCount == 1) {
         groupIndicatorBall.setColor(ReadySetGo.GREEN);
      }
      else if (((filledCount == 0) && (!commandGroup.isOptional())) || (filledCount > 1)) {
         groupIndicatorBall.setColor(ReadySetGo.RED);
      }
      else {
         groupIndicatorBall.setColor(ReadySetGo.YELLOW);
      }

      groupIter = indicatorMap.keySet().iterator();
      while (groupIter.hasNext()) {
         if ((indicatorMap.get(groupIter.next())).getColor().equals(ReadySetGo.RED)) {
            runnable = false;
            break;
         }
      }

      runAction.setEnabled(runnable);
   }

   public void caretUpdate (CaretEvent caretEvent) {

      setGroupIndicator(groupMap.get(caretEvent.getSource()));
   }

   public void itemStateChanged (ItemEvent itemEvent) {

      if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
         setGroupIndicator(groupMap.get(itemEvent.getSource()));
      }
   }

   public void windowOpened (WindowEvent windowEvent) {
   }

   public synchronized void windowClosing (WindowEvent windowEvent) {

      setVisible(false);
      dispose();
   }

   public void windowClosed (WindowEvent windowEvent) {
   }

   public void windowIconified (WindowEvent windowEvent) {
   }

   public void windowDeiconified (WindowEvent windowEvent) {
   }

   public void windowActivated (WindowEvent windowEvent) {
   }

   public void windowDeactivated (WindowEvent windowEvent) {
   }

   public class CancelAction extends AbstractAction {

      public CancelAction () {

         super();

         putValue(Action.NAME, "Cancel");
      }

      public synchronized void actionPerformed (ActionEvent actionEvent) {

         windowClosing(null);
      }

   }

   public class RunAction extends AbstractAction {

      public RunAction () {

         super();

         putValue(Action.NAME, "Run");
         setEnabled(false);
      }

      public synchronized void actionPerformed (ActionEvent actionEvent) {

         Iterator componentIter;
         Component commandComponent;

         outputCommandSet = new CommandSet();
         componentIter = groupMap.keySet().iterator();
         while (componentIter.hasNext()) {
            commandComponent = (Component)componentIter.next();
            if (isFilled(commandComponent)) {
               outputCommandSet.addArgument(nameMap.get(commandComponent), getArgument(commandComponent));
            }
         }

         windowClosing(null);
      }

   }

}
