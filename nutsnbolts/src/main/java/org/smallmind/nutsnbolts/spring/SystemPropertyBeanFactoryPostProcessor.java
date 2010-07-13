package org.smallmind.nutsnbolts.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.HashMap;
import java.util.Map;

public class SystemPropertyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

   private HashMap<String, String> propertyMap;
   private boolean override;

   public SystemPropertyBeanFactoryPostProcessor() {

      propertyMap = new HashMap<String, String>();
      override = false;
   }

   public void setOverride(boolean override) {

      this.override = override;
   }

   public void setPropertyMap(HashMap<String, String> propertyMap) {

      this.propertyMap.putAll(propertyMap);
   }

   public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

      for (Map.Entry<String, String> propertyEntry : propertyMap.entrySet()) {
         if (override || ((System.getProperty(propertyEntry.getKey()) == null) && (System.getenv(propertyEntry.getKey()) == null))) {
            System.setProperty(propertyEntry.getKey(), propertyEntry.getValue());
         }
      }
   }
}