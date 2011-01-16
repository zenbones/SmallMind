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
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.smallmind.liquibase.spring.Goal;
import org.smallmind.nutsnbolts.util.StringUtilities;

public class Liquidate extends JFrame {

   public Liquidate () {

      super("Liquidate");

      GroupLayout layout;
      GroupLayout.ParallelGroup buttonHorizontalGroup;
      GroupLayout.SequentialGroup buttonVerticalGroup;
      JComboBox databaseCombo;
      ButtonGroup goalButtonGroup;
      JRadioButton[] goalButtons;
      JTextField hostTextField;
      JTextField portTextField;
      JTextField userTextField;
      JTextField passwordTextField;
      JLabel databaseLabel;
      JLabel hostLabel;
      JLabel colonLabel;
      JLabel userLabel;
      JLabel passwordLabel;
      JLabel goalLabel;
      int goalIndex = 0;

      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      setLayout(layout = new GroupLayout(getContentPane()));

      databaseLabel = new JLabel("Choose Database:");
      databaseCombo = new JComboBox(Database.values());

      hostLabel = new JLabel("Choose Host and Port:");
      hostTextField = new JTextField();
      portTextField = new JTextField();
      portTextField.setHorizontalAlignment(JTextField.RIGHT);
      portTextField.setPreferredSize(new Dimension(50, (int)portTextField.getPreferredSize().getHeight()));
      portTextField.setMaximumSize(portTextField.getPreferredSize());
      colonLabel = new JLabel(":");

      userLabel = new JLabel("Choose User:");
      userTextField = new JTextField();

      passwordLabel = new JLabel("Choose Password:");
      passwordTextField = new JTextField();

      goalLabel = new JLabel("Choose Goal:");
      goalButtonGroup = new ButtonGroup();
      goalButtons = new JRadioButton[Goal.values().length - 1];
      for (Goal goal : Goal.values()) {
         if (!goal.equals(Goal.NONE)) {
            goalButtonGroup.add(goalButtons[goalIndex++] = new JRadioButton(StringUtilities.toDisplayCase(goal.name(), '_')));
         }
      }
      goalButtons[0].setSelected(true);

      layout.setAutoCreateContainerGaps(true);

      layout.setHorizontalGroup(layout.createSequentialGroup()
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup().addComponent(databaseLabel).addGap(10))
            .addGroup(layout.createSequentialGroup().addComponent(hostLabel).addGap(10))
            .addGroup(layout.createSequentialGroup().addComponent(userLabel).addGap(10))
            .addGroup(layout.createSequentialGroup().addComponent(passwordLabel).addGap(10))
            .addGroup(layout.createSequentialGroup().addComponent(goalLabel).addGap(10)))
         .addGroup(buttonHorizontalGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(databaseCombo).addComponent(userTextField).addComponent(passwordTextField)
            .addGroup(layout.createSequentialGroup().addComponent(hostTextField).addGap(2).addComponent(colonLabel).addGap(2).addComponent(portTextField))));

      for (JRadioButton goalButton : goalButtons) {
         buttonHorizontalGroup.addComponent(goalButton);
      }

      layout.setVerticalGroup(buttonVerticalGroup = layout.createSequentialGroup()
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(databaseLabel).addComponent(databaseCombo)).addGap(8)
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(hostLabel).addComponent(hostTextField).addComponent(colonLabel).addComponent(portTextField)).addGap(8)
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(userLabel).addComponent(userTextField)).addGap(8)
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(passwordLabel).addComponent(passwordTextField)).addGap(8)
         .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(goalLabel).addComponent(goalButtons[0])));

      for (int count = 1; count < goalButtons.length; count++) {
         buttonVerticalGroup.addComponent(goalButtons[count]);
      }

      setSize(getLayout().preferredLayoutSize(this));
   }

   public static void main (String... args) {

      Liquidate liquidate = new Liquidate();

      liquidate.setVisible(true);
   }
}
