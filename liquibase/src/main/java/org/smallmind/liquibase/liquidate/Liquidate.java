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

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class Liquidate extends JFrame {

   public Liquidate () {

      super("Liquidate");

      GroupLayout layout;
      JComboBox databaseCombo;
      JTextField hostTextField;
      JTextField portTextField;
      JLabel databaseLabel;
      JLabel hostLabel;
      JLabel colonLabel;

      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      setLayout(layout = new GroupLayout(getContentPane()));

      databaseLabel = new JLabel("Choose Database:");
      databaseCombo = new JComboBox(Database.values());

      hostLabel = new JLabel("Choose Host and Port:");
      hostTextField = new JTextField();
      portTextField = new JTextField();

      colonLabel = new JLabel(":");

      layout.setAutoCreateContainerGaps(true);

      layout.setHorizontalGroup(layout.createSequentialGroup().
         addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).addComponent(databaseLabel).addComponent(hostLabel)).
         addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(databaseCombo).
            addGroup(layout.createSequentialGroup().addComponent(hostTextField).addComponent(colonLabel).addComponent(portTextField))));
      layout.setVerticalGroup(layout.createSequentialGroup().
         addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(databaseLabel).addComponent(databaseCombo)).
         addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(hostLabel).addComponent(hostTextField).addComponent(colonLabel).addComponent(portTextField)));

      setSize(getLayout().minimumLayoutSize(this));
   }

   public static void main (String... args) {

      Liquidate liquidate = new Liquidate();

      liquidate.setVisible(true);
   }
}
