package org.smallmind.nutsnbolts.spring;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;

public class SystemPropertyInitializingBean implements InitializingBean {

   private HashMap<String, String> propertyMap;

   public SystemPropertyInitializingBean () {

      propertyMap = new HashMap<String, String>();
   }

   public void setPropertyMap (HashMap<String, String> propertyMap) {

      this.propertyMap.putAll(propertyMap);
   }

   public void afterPropertiesSet () {

      for (Map.Entry<String, String> propertyEntry : propertyMap.entrySet()) {
         System.setProperty(propertyEntry.getKey(), propertyEntry.getValue());
      }
   }
}