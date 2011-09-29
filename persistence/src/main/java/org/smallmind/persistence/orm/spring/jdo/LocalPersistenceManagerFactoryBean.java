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
package org.smallmind.persistence.orm.spring.jdo;

import java.util.Properties;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class LocalPersistenceManagerFactoryBean implements DisposableBean, FactoryBean<PersistenceManagerFactory>, InitializingBean {

   private PersistenceManagerFactory persistenceManagerFactory;
   private DataSource dataSource;
   private Properties properties;

   public void setDataSource (DataSource dataSource) {

      this.dataSource = dataSource;
   }

   public void setProperties (Properties properties) {

      this.properties = properties;
   }

   public PersistenceManagerFactory getObject () {

      return persistenceManagerFactory;
   }

   public Class getObjectType () {

      return PersistenceManagerFactory.class;
   }

   public boolean isSingleton () {

      return true;
   }

   public void destroy () {

      persistenceManagerFactory.close();
   }

   public void afterPropertiesSet () {

      persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(properties);
      persistenceManagerFactory.setConnectionFactory(dataSource);
   }
}
