package org.smallmind.nutsnbolts.spring;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
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
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringValueResolver;

public class PropertyPlaceholderConfigurer extends PropertyResourceConfigurer implements BeanFactoryAware, BeanNameAware {

   private BeanFactory beanFactory;
   private LinkedList<Resource> locationList = new LinkedList<Resource>();
   private String beanName;
   private SystemPropertyMode systemPropertyMode = SystemPropertyMode.FALLBACK;
   private boolean ignoreResourceNotFound = false;
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

   public void setIgnoreResourceNotFound (boolean ignoreResourceNotFound) {

      this.ignoreResourceNotFound = ignoreResourceNotFound;
   }

   public void setIgnoreUnresolvableProperties (boolean ignoreUnresolvableProperties) {

      this.ignoreUnresolvableProperties = ignoreUnresolvableProperties;
   }

   public void setSearchSystemEnvironment (boolean searchSystemEnvironment) {

      this.searchSystemEnvironment = searchSystemEnvironment;
   }

   public void setLocation (Resource location) {

      locationList.add(location);
   }

   public void setLocations (Resource[] locations) {

      locationList.addAll(Arrays.asList(locations));
   }

   protected void processProperties (ConfigurableListableBeanFactory beanFactoryToProcess, Properties properties)
      throws BeansException {

      Map<String, String> propertyMap;
      PropertyExpander locationExpander;
      StringValueResolver valueResolver;
      BeanDefinitionVisitor beanDefinitionVisitor;
      BeanDefinition beanDefinition;

      propertyMap = new HashMap<String, String>();

      try {
         locationExpander = new PropertyExpander(true, SystemPropertyMode.OVERRIDE, true);
      }
      catch (PropertyExpanderException propertyExpanderException) {
         throw new RuntimeBeansException(propertyExpanderException);
      }

      for (Resource location : locationList) {

         Properties locationProperties = new Properties();
         String locationPath;

         try {
            if (location instanceof FileSystemResource) {
               locationPath = locationExpander.expand(((FileSystemResource)location).getPath(), Collections.<String, String>emptyMap());
               locationProperties.load(new FileReader(new FileSystemResource(locationPath).getFile()));
            }
            else if (location instanceof ClassPathResource) {
               locationPath = locationExpander.expand(((ClassPathResource)location).getPath(), Collections.<String, String>emptyMap());
               locationProperties.load(new ClassPathResource(locationPath, ((ClassPathResource)location).getClassLoader()).getInputStream());
            }
            else if (location instanceof UrlResource) {
               locationPath = locationExpander.expand(location.getURL().toExternalForm(), Collections.<String, String>emptyMap());
               locationProperties.load(new UrlResource((new URL(locationPath))).getInputStream());
            }
            else {
               throw new RuntimeBeansException("Can't process resource(%s) of type(%s)", location.getDescription(), location.getClass());
            }
         }
         catch (PropertyExpanderException propertyExpanderException) {
            throw new RuntimeBeansException(propertyExpanderException);
         }
         catch (IOException ioException) {
            if (!ignoreResourceNotFound) {
               throw new RuntimeBeansException(ioException);
            }
         }

         for (Map.Entry propertyEntry : locationProperties.entrySet()) {
            propertyMap.put(propertyEntry.getKey().toString(), propertyEntry.getValue().toString());
         }
      }

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
