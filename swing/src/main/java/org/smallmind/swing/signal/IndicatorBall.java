package org.smallmind.swing.signal;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class IndicatorBall extends JLabel {

   private static ImageIcon RED_BALL;
   private static ImageIcon YELLOW_BALL;
   private static ImageIcon GREEN_BALL;

   private ReadySetGo color;

   static {

      RED_BALL = new ImageIcon(IndicatorBall.class.getClassLoader().getResource("public/iconexperience/bullets/16x16/plain/bullet_ball_glass_red.png"));
      YELLOW_BALL = new ImageIcon(IndicatorBall.class.getClassLoader().getResource("public/iconexperience/bullets/16x16/plain/bullet_ball_glass_yellow.png"));
      GREEN_BALL = new ImageIcon(IndicatorBall.class.getClassLoader().getResource("public/iconexperience/bullets/16x16/plain/bullet_ball_glass_green.png"));
   }

   public IndicatorBall (ReadySetGo color) {

      super();

      setColor(color);
   }

   public synchronized ReadySetGo getColor () {

      return color;
   }

   public synchronized void setColor (ReadySetGo color) {

      this.color = color;

      switch (color) {
         case RED:
            setIcon(RED_BALL);
            break;
         case YELLOW:
            setIcon(YELLOW_BALL);
            break;
         case GREEN:
            setIcon(GREEN_BALL);
            break;
         default:
            throw new UnknownSwitchCaseException(color.name());
      }
   }

}
