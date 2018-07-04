/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.persistence.orm.spring.jdo.antique.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

public class StandardPersistenceManagerProxyBean implements FactoryBean<PersistenceManager> {

  private PersistenceManager proxy;


  /**
   * Set the target JDO PersistenceManagerFactory that this proxy should
   * delegate to. This should be the raw PersistenceManagerFactory, as
   * accessed by JdoTransactionManager.
   */
  public void setPersistenceManagerFactory(PersistenceManagerFactory pmf) {
    Assert.notNull(pmf, "PersistenceManagerFactory must not be null");
    this.proxy = pmf.getPersistenceManagerProxy();
  }


  @Override
  public PersistenceManager getObject() {
    return this.proxy;
  }

  @Override
  public Class<? extends PersistenceManager> getObjectType() {
    return (this.proxy != null ? this.proxy.getClass() : PersistenceManager.class);
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
