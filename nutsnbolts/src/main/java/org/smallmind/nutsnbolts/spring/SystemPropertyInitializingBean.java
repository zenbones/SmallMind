/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.spring;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.smallmind.nutsnbolts.util.AlphaNumericComparator;
import org.smallmind.nutsnbolts.util.DotNotationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.PriorityOrdered;

public class SystemPropertyInitializingBean implements BeanFactoryPostProcessor, PriorityOrdered {

  private final TreeMap<String, String> debugMap = new TreeMap<String, String>(new AlphaNumericComparator<String>());
  private Map<String, String> propertyMap;
  private KeyDebugger keyDebugger;
  private boolean override;
  private int order;

  public SystemPropertyInitializingBean () {

    propertyMap = new HashMap<String, String>();
    override = false;
  }

  public SortedMap<String, String> getDebugMap () {

    return Collections.unmodifiableSortedMap(debugMap);
  }

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

      System.out.println("---------------- System Properties ---------------");
      for (Map.Entry<String, String> debugEntry : debugMap.entrySet()) {
        System.out.println("[" + debugEntry.getKey() + "=" + debugEntry.getValue() + "]");
      }
      System.out.println("--------------------------------------------------");
    }
  }
}