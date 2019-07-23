/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.spring;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.smallmind.nutsnbolts.property.PropertyExpander;
import org.smallmind.nutsnbolts.property.PropertyExpanderException;
import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.nutsnbolts.resource.ResourceParser;
import org.smallmind.nutsnbolts.resource.ResourceTypeFactory;
import org.smallmind.nutsnbolts.spring.property.PropertyEntry;
import org.smallmind.nutsnbolts.spring.property.PropertyFileType;
import org.smallmind.nutsnbolts.spring.property.PropertyHandler;
import org.smallmind.nutsnbolts.util.DotNotationComparator;
import org.smallmind.nutsnbolts.util.DotNotationException;
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
import org.springframework.core.PriorityOrdered;

public class PropertyPlaceholderConfigurer implements BeanFactoryPostProcessor, BeanFactoryAware, BeanNameAware, PriorityOrdered {

  private final TreeMap<String, String> debugMap = new TreeMap<>(new DotNotationComparator());
  private BeanFactory beanFactory;
  private KeyDebugger keyDebugger;
  private List<String> locations = new LinkedList<>();
  private String beanName;
  private SystemPropertyMode systemPropertyMode = SystemPropertyMode.FALLBACK;
  private boolean ignoreResourceNotFound = false;
  private boolean ignoreUnresolvableProperties = false;
  private boolean searchSystemEnvironment = true;
  private int order;

  public SortedMap<String, String> getDebugMap () {

    return Collections.unmodifiableSortedMap(debugMap);
  }

  public void setBeanFactory (BeanFactory beanFactory) {

    this.beanFactory = beanFactory;
  }

  public void setBeanName (String beanName) {

    this.beanName = beanName;
  }

  @Override
  public int getOrder () {

    return order;
  }

  public void setOrder (int order) {

    this.order = order;
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

  public void setLocations (List<String> locations) {

    this.locations = locations;
  }

  public void setDebugKeys (String[] debugPatterns)
    throws DotNotationException {

    keyDebugger = new KeyDebugger(debugPatterns);
  }

  @Override
  public void postProcessBeanFactory (ConfigurableListableBeanFactory beanFactoryToProcess)
    throws BeansException {

    PropertyPlaceholderStringValueResolver valueResolver;
    Map<String, Object> propertyMap = new HashMap<>();
    ResourceParser resourceParser;
    PropertyExpander locationExpander;
    BeanDefinitionVisitor beanDefinitionVisitor;
    BeanDefinition beanDefinition;

    resourceParser = new ResourceParser(new ResourceTypeFactory());

    try {
      locationExpander = new PropertyExpander(true, SystemPropertyMode.OVERRIDE, true);
    } catch (PropertyExpanderException propertyExpanderException) {
      throw new RuntimeBeansException(propertyExpanderException);
    }

    System.out.println("---------------- Property Loading ----------------");
    for (String location : locations) {
      extractProperties(resourceParser, locationExpander, propertyMap, location);
    }
    System.out.println("--------------------------------------------------");

    SpringPropertyAccessorManager.register(new SpringPropertyAccessor(valueResolver = new PropertyPlaceholderStringValueResolver(propertyMap, ignoreUnresolvableProperties, systemPropertyMode, searchSystemEnvironment)));

    if ((keyDebugger != null) && keyDebugger.willDebug()) {
      for (Map.Entry<String, Object> propertyEntry : propertyMap.entrySet()) {
        if (keyDebugger.matches(propertyEntry.getKey())) {

          Object value;

          debugMap.put(propertyEntry.getKey(), valueResolver.resolveStringValue(((value = propertyEntry.getValue()) == null) ? "" : value.toString()));
        }
      }

      System.out.println("---------------- Config Properties ---------------");
      for (Map.Entry<String, String> debugEntry : debugMap.entrySet()) {
        System.out.println("[" + debugEntry.getKey() + "=" + debugEntry.getValue() + "]");
      }
      System.out.println("--------------------------------------------------");
    }

    beanDefinitionVisitor = new BeanDefinitionVisitor(valueResolver);

    for (String beanName : beanFactoryToProcess.getBeanDefinitionNames()) {
      if ((!(beanName.equals(this.beanName)) && beanFactoryToProcess.equals(this.beanFactory))) {
        beanDefinition = beanFactoryToProcess.getBeanDefinition(beanName);
        try {
          beanDefinitionVisitor.visitBeanDefinition(beanDefinition);
        } catch (BeanDefinitionStoreException beanDefinitionStoreException) {
          throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName, beanDefinitionStoreException.getMessage());
        }
      }
    }

    beanFactoryToProcess.resolveAliases(valueResolver);
  }

  private void extractProperties (ResourceParser resourceParser, PropertyExpander locationExpander, Map<String, Object> propertyMap, String location) {

    Resource locationResource;
    InputStream inputStream;

    try {
      locationResource = resourceParser.parseResource(locationExpander.expand(location));
      if ((inputStream = locationResource.getInputStream()) == null) {
        throw new ResourceException("No stream available for resource(" + locationResource + ")");
      } else {

        PropertyHandler<?> propertyHandler;
        PropertyFileType propertyFileType;
        int lastDotPos;

        if ((lastDotPos = locationResource.getPath().lastIndexOf('.')) >= 0) {
          if ((propertyFileType = PropertyFileType.forExtension(locationResource.getPath().substring(lastDotPos + 1))) == null) {
            propertyFileType = PropertyFileType.PROPERTIES;
          }
        } else {
          propertyFileType = PropertyFileType.PROPERTIES;
        }

        propertyHandler = propertyFileType.getPropertyHandler(inputStream);
        System.out.println("[" + propertyFileType.name() + ":" + locationResource.getPath() + "]");
        for (PropertyEntry propertyEntry : propertyHandler) {
          propertyMap.put(propertyEntry.getKey(), propertyEntry.getValue());
        }
      }
    } catch (Exception exception) {
      if ((!ignoreResourceNotFound) || (!(exception instanceof ResourceException))) {
        throw new RuntimeBeansException(exception);
      }
    }
  }
}
