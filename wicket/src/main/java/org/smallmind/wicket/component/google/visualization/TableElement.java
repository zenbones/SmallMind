package org.smallmind.wicket.component.google.visualization;

import java.util.HashMap;

public class TableElement {

   private HashMap<String, String> propertyMap;

   public synchronized void addProperty (String key, String value) {

      if (propertyMap == null) {
         propertyMap = new HashMap<String, String>();
      }

      propertyMap.put(key, value);
   }

   public synchronized String getProperty (String key) {

      if (propertyMap != null) {

         return propertyMap.get(key);
      }

      return null;
   }
}
