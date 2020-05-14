/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.smallmind.nutsnbolts.util.AlphaNumericComparator;
import org.smallmind.nutsnbolts.util.DotNotationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.PriorityOrdered;

public class SystemPropertyInitializingBean implements BeanFactoryPostProcessor, PriorityOrdered {

  private final TreeMap<String, String> debugMap = new TreeMap<>(new AlphaNumericComparator<String>());
  private final Map<String, String> propertyMap = new HashMap<>();
  private KeyDebugger keyDebugger;
  private boolean override = false;
  private int order;

  @Override
  public int getOrder () {

    return order;
  }

  public void setOrder (int order) {

    this.order = order;
  }

  public void setOverride (boolean override) {

    this.override = override;
  }

  public void setPropertyMap (Map<String, String> propertyMap) {

    this.propertyMap.putAll(propertyMap);
  }

  public void setDebugKeys (String[] debugPatterns)
    throws DotNotationException {

    keyDebugger = new KeyDebugger(debugPatterns);
  }

  @Override
  // We exist as a post processor merely to get into the first 'special' initialization phase
  public void postProcessBeanFactory (ConfigurableListableBeanFactory beanFactory) throws BeansException {

    for (Map.Entry<String, String> propertyEntry : propertyMap.entrySet()) {
      if (override || ((System.getProperty(propertyEntry.getKey()) == null) && (System.getenv(propertyEntry.getKey()) == null))) {
        System.setProperty(propertyEntry.getKey(), propertyEntry.getValue());
      }

      if ((keyDebugger != null) && keyDebugger.willDebug()) {
        if (keyDebugger.matches(propertyEntry.getKey())) {

          String value;

          debugMap.put(propertyEntry.getKey(), ((value = System.getProperty(propertyEntry.getKey())) != null) ? value : System.getenv(propertyEntry.getKey()));
        }
      }
    }

    if ((keyDebugger != null) && keyDebugger.willDebug()) {
      debugMap.put("user.home", System.getProperty("user.home"));
      debugMap.put("java.io.tmpdir", System.getProperty("java.io.tmpdir"));

      System.out.println("---------------- System Properties ---------------");
      for (Map.Entry<String, String> debugEntry : debugMap.entrySet()) {
        System.out.println("[" + debugEntry.getKey() + "=" + debugEntry.getValue() + "]");
      }
      System.out.println("--------------------------------------------------");
    }
  }
}