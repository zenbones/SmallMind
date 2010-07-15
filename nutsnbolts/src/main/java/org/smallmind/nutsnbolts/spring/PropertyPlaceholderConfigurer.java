package org.smallmind.nutsnbolts.spring;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceParser;
import org.smallmind.nutsnbolts.resource.ResourceTypeFactory;
import org.smallmind.nutsnbolts.util.PropertyExpander;
import org.smallmind.nutsnbolts.util.PropertyExpanderException;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.StringValueResolver;

public class PropertyPlaceholderConfigurer implements BeanFactoryPostProcessor, BeanFactoryAware, BeanNameAware, Ordered, PriorityOrdered {

   private BeanFactory beanFactory;
   private LinkedList<String> locationList = new LinkedList<String>();
   private String beanName;
   private SystemPropertyMode systemPropertyMode = SystemPropertyMode.FALLBACK;
   private boolean ignoreResourceNotFound = false;
   private boolean ignoreUnresolvableProperties = false;
   private boolean searchSystemEnvironment = true;
   private int order;

   public void setBeanFactory (BeanFactory beanFactory) {

      this.beanFactory = beanFactory;
   }

   public void setBeanName (String beanName) {

      this.beanName = beanName;
   }

   public void setOrder (int order) {

      this.order = order;
   }

   public int getOrder () {

      return order;
   }

   public void setSystemPropertyMode (SystemPropertyMode systemPropertyMode) {

      this.systemPropertyMode = systemPropertyMode;
   }

   public void setIgnoreResourceNotFound (boolean ignoreResourceNotFound) {

      this.ignoreResourceNotFound = ignoreResourceNotFound;
   }

   public void setIgnoreUnresolvableProperties (boolean ignoreUnresolvableProperties) {

      this.ignoreUnresolvableProperties = ignoreUnresolvableProperties;
   }

   public void setSearchSystemEnvironment (boolean searchSystemEnvironment) {

      this.searchSystemEnvironment = searchSystemEnvironment;
   }

   public void setLocation (String location) {

      locationList.add(location);
   }

   public void setLocations (String[] locations) {

      locationList.addAll(Arrays.asList(locations));
   }

   public void postProcessBeanFactory (ConfigurableListableBeanFactory beanFactoryToProcess)
      throws BeansException {

      Map<String, String> propertyMap;
      ResourceParser resourceParser;
      PropertyExpander locationExpander;
      StringValueResolver valueResolver;
      BeanDefinitionVisitor beanDefinitionVisitor;
      BeanDefinition beanDefinition;

      resourceParser = new ResourceParser(new ResourceTypeFactory());
      propertyMap = new HashMap<String, String>();

      try {
         locationExpander = new PropertyExpander(true, SystemPropertyMode.OVERRIDE, true);
      }
      catch (PropertyExpanderException propertyExpanderException) {
         throw new RuntimeBeansException(propertyExpanderException);
      }

      for (String location : locationList) {

         Properties locationProperties = new Properties();
         Resource locationResource;
         InputStream inputStream;

         try {
            locationResource = resourceParser.parseResource(locationExpander.expand(location));
            if ((inputStream = locationResource.getInputStream()) == null) {
               throw new IOException("No stream available for resource(" + locationResource + ")");
            }
            else {
               locationProperties.load(inputStream);
            }
         }
         catch (Exception exception) {
            if ((!ignoreResourceNotFound) || (!(exception instanceof IOException))) {
               throw new RuntimeBeansException(exception);
            }
         }

         for (Map.Entry propertyEntry : locationProperties.entrySet()) {
            propertyMap.put(propertyEntry.getKey().toString(), propertyEntry.getValue().toString());
         }
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
