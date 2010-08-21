package org.smallmind.swing.panel;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.JPanel;

public class TabPanel extends JPanel {

   public TabPanel (Component component) {

      super(new GridBagLayout());

      GridBagConstraints constraint = new GridBagConstraints();
      JPanel insetPanel;

      insetPanel = new JPanel(new GridLayout(1, 0));
      insetPanel.add(component);

      constraint.anchor = GridBagConstraints.NORTHWEST;
      constraint.fill = GridBagConstraints.BOTH;
      constraint.insets = new Insets(5, 5, 5, 5);
      constraint.gridx = 0;
      constraint.gridy = 0;
      constraint.weightx = 1;
      constraint.weighty = 1;
      add(insetPanel, constraint);
   }

}
