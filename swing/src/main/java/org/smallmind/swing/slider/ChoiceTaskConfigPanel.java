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
package org.smallmind.swing.slider;

import java.awt.GridLayout;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class ChoiceTaskConfigPanel extends JPanel {

  public ChoiceTaskConfigPanel () {

    GroupLayout groupLayout;
    MultiThumbSlider choiceSlider;

    setLayout(groupLayout = new GroupLayout(this));

    choiceSlider = new MultiThumbSlider(MultiThumbSlider.VERTICAL);
    choiceSlider.setMinorTickSpacing(2);
    choiceSlider.setMajorTickSpacing(10);
    choiceSlider.addThumb(50);

    groupLayout.setHorizontalGroup(groupLayout.createParallelGroup().addComponent(choiceSlider));
    groupLayout.setVerticalGroup(groupLayout.createSequentialGroup().addComponent(choiceSlider));
  }

  public static void main (String... args) {

    JFrame frame = new JFrame();

    frame.getContentPane().setLayout(new GridLayout(1, 1));
    frame.getContentPane().add(new ChoiceTaskConfigPanel());

    frame.setSize(400, 400);
    frame.setVisible(true);
  }
}