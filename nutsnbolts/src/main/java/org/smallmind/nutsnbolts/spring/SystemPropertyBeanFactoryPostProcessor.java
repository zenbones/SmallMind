package org.smallmind.nutsnbolts.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.util.HashMap;
import java.util.Map;

public class SystemPropertyBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered, PriorityOrdered {

   private HashMap<String, String> propertyMap;
   private boolean override;
   private int order;

   public SystemPropertyBeanFactoryPostProcessor() {

      propertyMap = new HashMap<String, String>();
      override = false;
   }

   public void setOrder(int order) {

      this.order = order;
   }

   public void setOverride(boolean override) {

      this.override = override;
   }

   public void setPropertyMap(HashMap<String, String> propertyMap) {

      this.propertyMap.putAll(propertyMap);
   }

   public int getOrder() {

      return order;
   }

   public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

      for (Map.Entry<String, String> propertyEntry : propertyMap.entrySet()) {
         if (override || ((System.getProperty(propertyEntry.getKey()) == null) && (System.getenv(propertyEntry.getKey()) == null))) {
            System.setProperty(propertyEntry.getKey(), propertyEntry.getValue());
         }
      }
   }
}