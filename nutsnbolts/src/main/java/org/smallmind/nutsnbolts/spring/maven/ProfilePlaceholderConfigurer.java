/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.nutsnbolts.spring.maven;

import java.util.Properties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;

public class ProfilePlaceholderConfigurer extends PropertyResourceConfigurer implements BeanFactoryAware, BeanNameAware {

   private BeanFactory beanFactory;
   private String beanName;
   private String profile;
   private boolean ignoreResourceNotFound = false;
   private boolean ignoreUnresolvableProperties = false;

   public void setBeanFactory (BeanFactory beanFactory) {

      this.beanFactory = beanFactory;
   }

   public void setBeanName (String beanName) {

      this.beanName = beanName;
   }

   public void setProfile (String profile) {

      this.profile = profile;
   }

   public void setIgnoreResourceNotFound (boolean ignoreResourceNotFound) {

      this.ignoreResourceNotFound = ignoreResourceNotFound;
   }

   public void setIgnoreUnresolvableProperties (boolean ignoreUnresolvableProperties) {

      this.ignoreUnresolvableProperties = ignoreUnresolvableProperties;
   }

   @Override
   protected void processProperties (ConfigurableListableBeanFactory beanFactoryToProcess, Properties properties)
      throws BeansException {

      ProfilePropertyStringValueResolver valueResolver;
      BeanDefinitionVisitor beanDefinitionVisitor;
      BeanDefinition beanDefinition;

      if ((valueResolver = new ProfilePropertyStringValueResolver(profile, ignoreResourceNotFound, ignoreUnresolvableProperties)).isActive()) {
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
}
