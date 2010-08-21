package org.smallmind.swing.cycle;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListModel;
import org.smallmind.swing.ComponentUtilities;
import org.smallmind.nutsnbolts.util.Counter;

public class Cycle extends JPanel {

   private static final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();

   private static ImageIcon CYCLE_UP;
   private static ImageIcon CYCLE_DOWN;

   private final Counter indexCounter;

   private ListModel listModel;
   private CycleRenderer renderer;
   private CycleRubberStamp rubberStamp;
   private JLabel numericLabel;
   private Color unselectedBackgroundColor;
   private Color unselectedForegroundColor;
   private Color selectedBackgroundColor;
   private Color selectedForegroundColor;
   private boolean selected = false;

   static {

      CYCLE_UP = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/arrow_right_blue.png"));
      CYCLE_DOWN = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/arrow_left_blue.png"));
   }

   public Cycle (ListModel listModel) {

      super(GRID_BAG_LAYOUT);

      GridBagConstraints constraint;
      JButton cycleUpButton;
      JButton cycleDownButton;

      this.listModel = listModel;

      unselectedBackgroundColor = SystemColor.text;
      unselectedForegroundColor = SystemColor.textText;
      selectedBackgroundColor = SystemColor.textHighlight;
      selectedForegroundColor = SystemColor.text;

      indexCounter = new Counter((listModel.getSize() > 0) ? 1 : 0);
      renderer = new DefaultCycleRenderer();

      numericLabel = new JLabel(String.valueOf(indexCounter.getCount()));
      numericLabel.setOpaque(true);
      numericLabel.setBackground(unselectedBackgroundColor);
      numericLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createLineBorder(unselectedBackgroundColor, 2)));

      cycleUpButton = new JButton(new CycleUpAction());
      cycleUpButton.setFocusable(false);
      ComponentUtilities.setPreferredWidth(cycleUpButton, 18);
      ComponentUtilities.setMinimumWidth(cycleUpButton, 18);
      ComponentUtilities.setMaximumWidth(cycleUpButton, 18);

      cycleDownButton = new JButton(new CycleDownAction());
      cycleDownButton.setFocusable(false);
      ComponentUtilities.setPreferredWidth(cycleDownButton, 18);
      ComponentUtilities.setMinimumWidth(cycleDownButton, 18);
      ComponentUtilities.setMaximumWidth(cycleDownButton, 18);

      rubberStamp = new CycleRubberStamp(this);

      constraint = new GridBagConstraints();

      constraint.anchor = GridBagConstraints.WEST;
      constraint.fill = GridBagConstraints.BOTH;
      constraint.gridx = 0;
      constraint.gridy = 0;
      constraint.weightx = 1;
      constraint.weighty = 1;
      add(rubberStamp, constraint);

      constraint.anchor = GridBagConstraints.WEST;
      constraint.fill = GridBagConstraints.VERTICAL;
      constraint.gridx = 1;
      constraint.gridy = 0;
      constraint.weightx = 0;
      constraint.weighty = 1;
      add(cycleDownButton, constraint);

      constraint.anchor = GridBagConstraints.WEST;
      constraint.fill = GridBagConstraints.VERTICAL;
      constraint.gridx = 2;
      constraint.gridy = 0;
      constraint.weightx = 0;
      constraint.weighty = 1;
      add(numericLabel, constraint);

      constraint.anchor = GridBagConstraints.WEST;
      constraint.fill = GridBagConstraints.VERTICAL;
      constraint.gridx = 3;
      constraint.gridy = 0;
      constraint.weightx = 0;
      constraint.weighty = 1;
      add(cycleUpButton, constraint);

      updateRenderPanel();
   }

   public int getIndex () {

      synchronized (indexCounter) {
         return indexCounter.getCount();
      }
   }

   public void setCycleRenderer (CycleRenderer renderer) {

      this.renderer = renderer;
      updateRenderPanel();
   }

   public Color getUnselectedBackgroundColor () {

      return unselectedBackgroundColor;
   }

   public void setUnselectedBackgroundColor (Color unselectedBackgroundColor) {

      this.unselectedBackgroundColor = unselectedBackgroundColor;
   }

   public Color getUnselectedForegroundColor () {

      return unselectedForegroundColor;
   }

   public void setUnselectedForegroundColor (Color unselectedForegroundColor) {

      this.unselectedForegroundColor = unselectedForegroundColor;
   }

   public Color getSelectedBackgroundColor () {

      return selectedBackgroundColor;
   }

   public void setSelectedBackgroundColor (Color selectedBackgroundColor) {

      this.selectedBackgroundColor = selectedBackgroundColor;
   }

   public Color getSelectedForegroundColor () {

      return selectedForegroundColor;
   }

   public void setSelectedForegroundColor (Color selectedForegroundColor) {

      this.selectedForegroundColor = selectedForegroundColor;
   }

   public synchronized void setSelected (boolean selected) {

      this.selected = selected;

      numericLabel.setBackground((selected) ? selectedBackgroundColor : unselectedBackgroundColor);
      numericLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createLineBorder((selected) ? selectedBackgroundColor : unselectedBackgroundColor, 2)));
      numericLabel.setForeground((selected) ? selectedForegroundColor : unselectedForegroundColor);

      updateRenderPanel();
   }

   public synchronized Component getRenderComponent () {

      return renderer.getCycleRendererComponent(this, listModel.getElementAt(indexCounter.getCount() - 1), indexCounter.getCount() - 1, selected);
   }

   private void updateRenderPanel () {

      synchronized (indexCounter) {
         if (indexCounter.getCount() > 0) {
            rubberStamp.repaint();
         }
      }
   }

   public class CycleUpAction extends AbstractAction {

      public CycleUpAction () {

         super();

         putValue(Action.SMALL_ICON, CYCLE_UP);
      }

      public void actionPerformed (ActionEvent actionEvent) {

         synchronized (indexCounter) {
            if (indexCounter.getCount() < listModel.getSize()) {
               numericLabel.setText(String.valueOf(indexCounter.inc()));
               updateRenderPanel();
            }
         }
      }

   }

   public class CycleDownAction extends AbstractAction {

      public CycleDownAction () {

         super();

         putValue(Action.SMALL_ICON, CYCLE_DOWN);
      }

      public void actionPerformed (ActionEvent actionEvent) {

         synchronized (indexCounter) {
            if (indexCounter.getCount() > 1) {
               numericLabel.setText(String.valueOf(indexCounter.dec()));
               updateRenderPanel();
            }
         }
      }

   }

}
