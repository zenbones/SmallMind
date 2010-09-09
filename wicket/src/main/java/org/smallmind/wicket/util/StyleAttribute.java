package org.smallmind.wicket.util;

import java.util.HashMap;
import java.util.Map;

public class StyleAttribute {

   private HashMap<String, String> styleMap = new HashMap<String, String>();

   public StyleAttribute (String style) {

      int colonPos;

      if (style != null) {
         for (String styleSegment : style.split(";", -1)) {
            if ((colonPos = styleSegment.indexOf(":")) < 0) {
               throw new IllegalArgumentException(style);
            }

            styleMap.put(styleSegment.substring(0, colonPos).trim(), styleSegment.substring(colonPos + 1).trim());
         }
      }
   }

   public synchronized String getAttribute (String styleKey) {

      return styleMap.get(styleKey);
   }

   public synchronized void putAttribute (String styleKey, String styleValue) {

      styleMap.put(styleKey, styleValue);
   }

   public synchronized String getStyle () {

      StringBuilder styleBuilder = new StringBuilder();

      for (Map.Entry<String, String> entry : styleMap.entrySet()) {
         if (styleBuilder.length() > 0) {
            styleBuilder.append(';');
         }
         styleBuilder.append(entry.getKey()).append(": ").append(entry.getValue());
      }

      return styleBuilder.toString();
   }
}