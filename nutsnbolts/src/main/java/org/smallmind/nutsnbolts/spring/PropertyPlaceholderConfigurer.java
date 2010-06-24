package org.smallmind.nutsnbolts.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.util.StringValueResolver;

public class PropertyPlaceholderConfigurer extends PropertyResourceConfigurer implements BeanFactoryAware, BeanNameAware {

   private BeanFactory beanFactory;
   private String beanName;
   private SystemPropertyMode systemPropertyMode = SystemPropertyMode.FALLBACK;
   private boolean ignoreUnresolvableProperties = false;
   private boolean searchSystemEnvironment = true;

   public void setBeanFactory (BeanFactory beanFactory) {

      this.beanFactory = beanFactory;
   }

   public void setBeanName (String beanName) {

      this.beanName = beanName;
   }

   public void setSystemPropertyMode (SystemPropertyMode systemPropertyMode) {

      this.systemPropertyMode = systemPropertyMode;
   }

   public void setIgnoreUnresolvableProperties (boolean ignoreUnresolvableProperties) {

      this.ignoreUnresolvableProperties = ignoreUnresolvableProperties;
   }

   public void setSearchSystemEnvironment (boolean searchSystemEnvironment) {

      this.searchSystemEnvironment = searchSystemEnvironment;
   }

   protected void processProperties (ConfigurableListableBeanFactory beanFactoryToProcess, Properties properties)
      throws BeansException {

      Map<String, String> propertyMap;
      StringValueResolver valueResolver;
      BeanDefinitionVisitor beanDefinitionVisitor;
      BeanDefinition beanDefinition;

      propertyMap = new HashMap<String, String>();
      for (Map.Entry propertyEntry : properties.entrySet()) {
         propertyMap.put(propertyEntry.getKey().toString(), propertyEntry.getValue().toString());
      }

      valueResolver = new PropertyPlaceholderStringValueResolver(propertyMap, ignoreUnresolvableProperties, systemPropertyMode, searchSystemEnvironment);
      beanDefinitionVisitor = new BeanDefinitionVisitor(valueResolver);

      for (String beanName : beanFactoryToProcess.getBeanDefinitionNames()) {
         if ((!(beanName.equals(this.beanName)) && beanFactoryToProcess.equals(this.beanFactory))) {
            beanDefinition = beanFactoryToProcess.getBeanDefinition(beanName);
            try {
               beanDefinitionVisitor.visitBeanDefinition(beanDefinition);
            }
            catch (BeanDefinitionStoreException beanDefinitionStoreException) {
               throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName, beanDefinitionStoreException.getMessage());
            }
         }
      }

      beanFactoryToProcess.resolveAliases(valueResolver);
   }
}
