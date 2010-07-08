package org.smallmind.nutsnbolts.spring;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;

public class SystemPropertyInitializingBean implements InitializingBean {

   private HashMap<String, String> propertyMap;
   private boolean override;

   public SystemPropertyInitializingBean () {

      propertyMap = new HashMap<String, String>();
      override = false;
   }

   public void setOverride (boolean override) {

      this.override = override;
   }

   public void setPropertyMap (HashMap<String, String> propertyMap) {

      this.propertyMap.putAll(propertyMap);
   }

   public void afterPropertiesSet () {

      for (Map.Entry<String, String> propertyEntry : propertyMap.entrySet()) {
         if (override || (System.getProperty(propertyEntry.getKey()) == null)) {
            System.setProperty(propertyEntry.getKey(), propertyEntry.getValue());
         }
      }
   }
}