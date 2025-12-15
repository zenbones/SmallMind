/*
 * Copyright (c) 2007 through 2026 David Berkman
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

/**
 * Initializes JVM system properties early in the Spring lifecycle, with optional override and debug logging.
 */
public class SystemPropertyInitializingBean implements BeanFactoryPostProcessor, PriorityOrdered {

  private final TreeMap<String, String> debugMap = new TreeMap<>(new AlphaNumericComparator<String>());
  private final Map<String, String> propertyMap = new HashMap<>();
  private KeyDebugger keyDebugger;
  private boolean override = false;
  private int order;

  /**
   * {@inheritDoc}
   */
  @Override
  public int getOrder () {

    return order;
  }

  /**
   * Sets the priority order.
   *
   * @param order priority value
   */
  public void setOrder (int order) {

    this.order = order;
  }

  /**
   * Controls whether configured properties override existing system or environment values.
   *
   * @param override {@code true} to override existing values
   */
  public void setOverride (boolean override) {

    this.override = override;
  }

  /**
   * Adds properties to be applied as system properties.
   *
   * @param propertyMap properties to set
   */
  public void setPropertyMap (Map<String, String> propertyMap) {

    this.propertyMap.putAll(propertyMap);
  }

  /**
   * Enables debug logging for keys matching the supplied patterns.
   *
   * @param debugPatterns include/exclude patterns
   * @throws DotNotationException if a pattern is invalid
   */
  public void setDebugKeys (String[] debugPatterns)
    throws DotNotationException {

    keyDebugger = new KeyDebugger(debugPatterns);
  }

  @Override
  // We exist as a post processor merely to get into the first 'special' initialization phase
  /**
   * Applies configured system properties and logs selected keys when enabled.
   *
   * @param beanFactory the bean factory (unused)
   * @throws BeansException if property setting fails
   */
  public void postProcessBeanFactory (ConfigurableListableBeanFactory beanFactory)
    throws BeansException {

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
      debugMap.put("spring.profiles.active", System.getProperty("spring.profiles.active"));

      System.out.println("---------------- System Properties ---------------");
      for (Map.Entry<String, String> debugEntry : debugMap.entrySet()) {
        System.out.println("[" + debugEntry.getKey() + "=" + ((debugEntry.getValue() == null) ? "null" : debugEntry.getValue()) + "]");
      }
      System.out.println("--------------------------------------------------");
    }
  }
}
