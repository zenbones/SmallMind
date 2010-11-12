/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
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

import java.util.HashMap;
import java.util.Map;
import org.smallmind.nutsnbolts.util.DotNotationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

public class SystemPropertyBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered, PriorityOrdered {

   private HashMap<String, String> propertyMap;
   private KeyDebugger keyDebugger;
   private boolean override;
   private int order;

   public SystemPropertyBeanFactoryPostProcessor () {

      propertyMap = new HashMap<String, String>();
      override = false;
   }

   public void setOrder (int order) {

      this.order = order;
   }

   public int getOrder () {

      return order;
   }

   public void setOverride (boolean override) {

      this.override = override;
   }

   public void setPropertyMap (HashMap<String, String> propertyMap) {

      this.propertyMap.putAll(propertyMap);
   }

   public void setDebugKeys (String[] debugPatterns)
      throws DotNotationException {

      keyDebugger = new KeyDebugger(debugPatterns);
   }

   public void postProcessBeanFactory (ConfigurableListableBeanFactory configurableListableBeanFactory)
      throws BeansException {

      if (keyDebugger.willDebug()) {
         System.out.println("---------------- System Properties ---------------");
      }

      for (Map.Entry<String, String> propertyEntry : propertyMap.entrySet()) {
         if (override || ((System.getProperty(propertyEntry.getKey()) == null) && (System.getenv(propertyEntry.getKey()) == null))) {
            System.setProperty(propertyEntry.getKey(), propertyEntry.getValue());
            if (keyDebugger.willDebug() && keyDebugger.matches(propertyEntry.getKey())) {
               System.out.println("[" + propertyEntry.getKey() + "=" + propertyEntry.getValue() + "]");
            }
         }
      }

      if (keyDebugger.willDebug()) {
         System.out.println("--------------------------------------------------");
      }
   }
}