package org.smallmind.nutsnbolts.swing.banner;

import java.awt.Component;

public interface BannerRenderer {

   public abstract Component getBannerRendererComponent (Banner banner, Object value, int index, boolean isSelected);

}
