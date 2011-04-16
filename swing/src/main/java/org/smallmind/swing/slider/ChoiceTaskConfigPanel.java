package org.smallmind.swing.slider;

import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class ChoiceTaskConfigPanel extends JPanel {

  public ChoiceTaskConfigPanel () {

    GroupLayout groupLayout;
    MultiThumbSlider choiceSlider;

    JSlider s = new JSlider(JSlider.HORIZONTAL);
    s.setMaximum(1234567);
    s.setMinimum(0);
    s.setPaintTicks(true);
    s.setSnapToTicks(true);
    s.setMajorTickSpacing(600000);
    s.setMinorTickSpacing(200000);
    s.setPaintLabels(true);

    setLayout(groupLayout = new GroupLayout(this));

    choiceSlider = new MultiThumbSlider();

    groupLayout.setHorizontalGroup(groupLayout.createParallelGroup().addComponent(s).addComponent(choiceSlider));
    groupLayout.setVerticalGroup(groupLayout.createSequentialGroup().addComponent(s).addComponent(choiceSlider));
  }

  public static void main (String... args) {

    JFrame frame = new JFrame();

    frame.getContentPane().setLayout(new GridLayout(1, 1));
    frame.getContentPane().add(new ChoiceTaskConfigPanel());

    frame.setSize(400, 400);
    frame.setVisible(true);
  }
}