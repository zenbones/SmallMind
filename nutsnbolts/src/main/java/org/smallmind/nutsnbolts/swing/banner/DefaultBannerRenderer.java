package org.smallmind.nutsnbolts.swing.banner;

import java.awt.Component;
import java.awt.SystemColor;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

public class DefaultBannerRenderer implements BannerRenderer {

   private JLabel renderLabel;

   public DefaultBannerRenderer () {

      renderLabel = new JLabel();
      renderLabel.setOpaque(true);
   }

   public Component getBannerRendererComponent (Banner banner, Object value, int index, boolean isSelected) {

      renderLabel.setText(value.toString());
      renderLabel.setBackground((isSelected) ? SystemColor.textHighlight : SystemColor.text);
      renderLabel.setForeground((isSelected) ? SystemColor.text : SystemColor.textText);
      renderLabel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, (isSelected) ? SystemColor.textHighlight : SystemColor.text));

      return renderLabel;
   }

}
