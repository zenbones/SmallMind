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
package org.smallmind.liquibase.liquidate;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import org.smallmind.liquibase.spring.Goal;
import org.smallmind.liquibase.spring.Source;
import org.smallmind.liquibase.spring.SpringLiquibase;
import org.smallmind.nutsnbolts.util.StringUtilities;
import org.smallmind.persistence.orm.sql.DriverManagerDataSource;

public class Liquidate extends JFrame implements ActionListener {

   private JComboBox databaseCombo;
   private ButtonGroup sourceButtonGroup;
   private ButtonGroup goalButtonGroup;
   private JPasswordField passwordField;
   private JTextField hostTextField;
   private JTextField portTextField;
   private JTextField schemaTextField;
   private JTextField userTextField;
   private JTextField logTextField;

   public Liquidate () {

      super("Liquidate");

      GroupLayout layout;
      GroupLayout.ParallelGroup sourceVerticalGroup;
      GroupLayout.ParallelGroup goalHorizontalGroup;
      GroupLayout.SequentialGroup sourceHorizontalGroup;
      GroupLayout.SequentialGroup goalVerticalGroup;
      JSeparator buttonSeparator;
      JButton startButton;
      JRadioButton[] sourceButtons;
      JRadioButton[] goalButtons;
      JLabel databaseLabel;
      JLabel hostLabel;
      JLabel colonLabel;
      JLabel schemaLabel;
      JLabel userLabel;
      JLabel passwordLabel;
      JLabel sourceLabel;
      JLabel goalLabel;
      int sourceIndex = 0;
      int goalIndex = 0;

      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      setLayout(layout = new GroupLayout(getContentPane()));

      databaseLabel = new JLabel("Database:");
      databaseCombo = new JComboBox(Database.values());

      hostLabel = new JLabel("Host and Port:");
      hostTextField = new JTextField();
      portTextField = new JTextField();
      portTextField.setHorizontalAlignment(JTextField.RIGHT);
      portTextField.setPreferredSize(new Dimension(50, (int)portTextField.getPreferredSize().getHeight()));
      portTextField.setMaximumSize(portTextField.getPreferredSize());
      colonLabel = new JLabel(":");

      schemaLabel = new JLabel("Schema:");
      schemaTextField = new JTextField();

      userLabel = new JLabel("User:");
      userTextField = new JTextField();

      passwordLabel = new JLabel("Password:");
      passwordField = new JPasswordField();

      sourceLabel = new JLabel("Change Log:");
      sourceButtonGroup = new ButtonGroup();
      sourceButtons = new JRadioButton[Source.values().length];
      for (Source source : Source.values()) {
         sourceButtonGroup.add(sourceButtons[sourceIndex] = new JRadioButton(StringUtilities.toDisplayCase(source.name(), '_')));
         sourceButtons[sourceIndex++].setActionCommand(source.name());
      }
      sourceButtons[0].setSelected(true);
      logTextField = new JTextField();

      goalLabel = new JLabel("Goal:");
      goalButtonGroup = new ButtonGroup();
      goalButtons = new JRadioButton[Goal.values().length - 1];
      for (Goal goal : Goal.values()) {
         if (!goal.equals(Goal.NONE)) {
            goalButtonGroup.add(goalButtons[goalIndex] = new JRadioButton(StringUtilities.toDisplayCase(goal.name(), '_')));
            goalButtons[goalIndex++].setActionCommand(goal.name());
         }
      }
      goalButtons[0].setSelected(true);

      buttonSeparator = new JSeparator(JSeparator.HORIZONTAL);
      buttonSeparator.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int)buttonSeparator.getPreferredSize().getHeight()));
      startButton = new JButton("Start");
      startButton.addActionListener(this);

      layout.setAutoCreateContainerGaps(true);

      layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
         .addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup().addComponent(databaseLabel).addGap(10))
            .addGroup(layout.createSequentialGroup().addComponent(hostLabel).addGap(10))
            .addGroup(layout.createSequentialGroup().addComponent(schemaLabel).addGap(10))
            .addGroup(layout.createSequentialGroup().addComponent(userLabel).addGap(10))
            .addGroup(layout.createSequentialGroup().addComponent(passwordLabel).addGap(10))
            .addGroup(layout.createSequentialGroup().addComponent(sourceLabel).addGap(10))
            .addGroup(layout.createSequentialGroup().addComponent(goalLabel).addGap(10)))
            .addGroup(goalHorizontalGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(databaseCombo)
               .addGroup(layout.createSequentialGroup().addComponent(hostTextField).addGap(2).addComponent(colonLabel).addGap(2).addComponent(portTextField))
               .addComponent(schemaTextField).addComponent(userTextField).addComponent(passwordField)
               .addGroup(sourceHorizontalGroup = layout.createSequentialGroup()).addComponent(logTextField)))
         .addComponent(buttonSeparator).addComponent(startButton));

      for (JRadioButton sourceButton : sourceButtons) {
         sourceHorizontalGroup.addComponent(sourceButton);
      }

      for (JRadioButton goalButton : goalButtons) {
         goalHorizontalGroup.addComponent(goalButton);
      }

      layout.setVerticalGroup(goalVerticalGroup = layout.createSequentialGroup()
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(databaseLabel).addComponent(databaseCombo)).addGap(8)
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(hostLabel).addComponent(hostTextField).addComponent(colonLabel).addComponent(portTextField)).addGap(8)
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(schemaLabel).addComponent(schemaTextField)).addGap(8)
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(userLabel).addComponent(userTextField)).addGap(8)
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(passwordLabel).addComponent(passwordField)).addGap(8)
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(sourceLabel).addGroup(sourceVerticalGroup = layout.createParallelGroup())).addGap(8)
         .addComponent(logTextField).addGap(8)
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(goalLabel).addComponent(goalButtons[0])));

      for (JRadioButton sourceButton : sourceButtons) {
         sourceVerticalGroup.addComponent(sourceButton);
      }

      for (int count = 1; count < goalButtons.length; count++) {
         goalVerticalGroup.addComponent(goalButtons[count]);
      }

      goalVerticalGroup.addGap(15).addComponent(buttonSeparator).addGap(8).addComponent(startButton);

      setSize(new Dimension(((int)getLayout().preferredLayoutSize(this).getWidth()) + 120, ((int)getLayout().preferredLayoutSize(this).getHeight()) + 35));
      setLocationByPlatform(true);
   }

   public void actionPerformed (ActionEvent actionEvent) {

      SpringLiquibase springLiquibase;
      Database database;

      springLiquibase = new SpringLiquibase();
      springLiquibase.setSource(Source.valueOf(sourceButtonGroup.getSelection().getActionCommand()));
      springLiquibase.setChangeLog(logTextField.getText());
      springLiquibase.setGoal(Goal.valueOf(goalButtonGroup.getSelection().getActionCommand()));

      database = (Database)databaseCombo.getSelectedItem();

      try {
         springLiquibase.setDataSource(new DriverManagerDataSource(database.getDriver().getName(), database.getUrl(hostTextField.getText(), portTextField.getText(), schemaTextField.getText()), userTextField.getText(), new String(passwordField.getPassword())));
         springLiquibase.afterPropertiesSet();
      }
      catch (Exception exception) {
         throw new RuntimeException(exception);
      }

      this.setVisible(false);
      this.dispose();
   }

   public void setDatabase (Database database) {

      databaseCombo.setSelectedItem(database);
   }

   public void setHost (String host) {

      hostTextField.setText(host);
   }

   public void setPort (int port) {

      portTextField.setText(String.valueOf(port));
   }

   public void setSchema (String schema) {

      schemaTextField.setText(schema);
   }

   public void setUser (String user) {

      userTextField.setText(user);
   }

   public void setPassword (String password) {

      passwordField.setText(password);
   }

   public void setSource (Source source) {

      Enumeration<AbstractButton> buttonEnum = sourceButtonGroup.getElements();
      AbstractButton button;

      while (buttonEnum.hasMoreElements()) {
         if ((button = buttonEnum.nextElement()).getActionCommand().equals(source.name())) {
            sourceButtonGroup.setSelected(button.getModel(), true);
            break;
         }
      }
   }

   public void setChangeLog (String changeLog) {

      logTextField.setText(changeLog);
   }

   public void setGoal (Goal goal) {

      Enumeration<AbstractButton> buttonEnum = goalButtonGroup.getElements();
      AbstractButton button;

      while (buttonEnum.hasMoreElements()) {
         if ((button = buttonEnum.nextElement()).getActionCommand().equals(goal.name())) {
            goalButtonGroup.setSelected(button.getModel(), true);
            break;
         }
      }
   }
}
