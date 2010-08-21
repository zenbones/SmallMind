package org.smallmind.swing.signal;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class TrafficLight extends Box {

   public static final int X_AXIS = BoxLayout.X_AXIS;
   public static final int Y_AXIS = BoxLayout.Y_AXIS;

   private static ImageIcon CLEAR_LIGHT;
   private static ImageIcon RED_LIGHT;
   private static ImageIcon YELLOW_LIGHT;
   private static ImageIcon GREEN_LIGHT;

   private JLabel redLightLabel;
   private JLabel yellowLightLabel;
   private JLabel greenLightLabel;
   private ReadySetGo readySetGo;

   static {

      CLEAR_LIGHT = new ImageIcon(ClassLoader.getSystemResource("public/images/bullet_ball_glass_clear.png"));
      RED_LIGHT = new ImageIcon(ClassLoader.getSystemResource("public/images/bullet_ball_glass_red.png"));
      YELLOW_LIGHT = new ImageIcon(ClassLoader.getSystemResource("public/images/bullet_ball_glass_yellow.png"));
      GREEN_LIGHT = new ImageIcon(ClassLoader.getSystemResource("public/images/bullet_ball_glass_green.png"));
   }

   public TrafficLight (int axis) {

      super(axis);

      readySetGo = ReadySetGo.RED;

      redLightLabel = new JLabel(RED_LIGHT);
      yellowLightLabel = new JLabel(CLEAR_LIGHT);
      greenLightLabel = new JLabel(CLEAR_LIGHT);

      if (axis == X_AXIS) {
         add(redLightLabel);
         add(createHorizontalStrut(3));
         add(yellowLightLabel);
         add(createHorizontalStrut(3));
         add(greenLightLabel);
      }
      else {
         add(greenLightLabel);
         add(createVerticalStrut(3));
         add(yellowLightLabel);
         add(createVerticalStrut(3));
         add(redLightLabel);
      }
   }

   public synchronized void inc () {

      readySetGo = readySetGo.inc();

      switch (readySetGo) {
         case YELLOW:
            redLightLabel.setIcon(CLEAR_LIGHT);
            yellowLightLabel.setIcon(YELLOW_LIGHT);
            break;
         case GREEN:
            yellowLightLabel.setIcon(CLEAR_LIGHT);
            greenLightLabel.setIcon(GREEN_LIGHT);
      }
   }

}
