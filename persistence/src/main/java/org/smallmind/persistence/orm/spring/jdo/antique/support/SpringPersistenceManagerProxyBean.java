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

import org.smallmind.persistence.orm.spring.jdo.antique.DefaultJdoDialect;
import org.smallmind.persistence.orm.spring.jdo.antique.JdoDialect;
import org.smallmind.persistence.orm.spring.jdo.antique.PersistenceManagerFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class SpringPersistenceManagerProxyBean implements FactoryBean<PersistenceManager>, InitializingBean {

  private PersistenceManagerFactory persistenceManagerFactory;

  private JdoDialect jdoDialect;

  private Class<? extends PersistenceManager> persistenceManagerInterface = PersistenceManager.class;

  private boolean allowCreate = true;

  private PersistenceManager proxy;

  /**
   * Return the target PersistenceManagerFactory for this proxy.
   */
  protected PersistenceManagerFactory getPersistenceManagerFactory() {
    return this.persistenceManagerFactory;
  }

  /**
   * Set the target PersistenceManagerFactory for this proxy.
   */
  public void setPersistenceManagerFactory(PersistenceManagerFactory persistenceManagerFactory) {
    this.persistenceManagerFactory = persistenceManagerFactory;
  }

  /**
   * Return the JDO dialect to use for this proxy.
   */
  protected JdoDialect getJdoDialect() {
    return this.jdoDialect;
  }

  /**
   * Set the JDO dialect to use for this proxy.
   * <p>Default is a DefaultJdoDialect based on the PersistenceManagerFactory's
   * underlying DataSource, if any.
   */
  public void setJdoDialect(JdoDialect jdoDialect) {
    this.jdoDialect = jdoDialect;
  }

  /**
   * Return the PersistenceManager interface to expose.
   */
  protected Class<? extends PersistenceManager> getPersistenceManagerInterface() {
    return this.persistenceManagerInterface;
  }

  /**
   * Specify the PersistenceManager interface to expose,
   * possibly including vendor extensions.
   * <p>Default is the standard {@code javax.jdo.PersistenceManager} interface.
   */
  public void setPersistenceManagerInterface(Class<? extends PersistenceManager> persistenceManagerInterface) {
    this.persistenceManagerInterface = persistenceManagerInterface;
    Assert.notNull(persistenceManagerInterface, "persistenceManagerInterface must not be null");
    Assert.isAssignable(PersistenceManager.class, persistenceManagerInterface);
  }

  /**
   * Return whether the PersistenceManagerFactory proxy is allowed to create
   * a non-transactional PersistenceManager when no transactional
   * PersistenceManager can be found for the current thread.
   */
  protected boolean isAllowCreate() {
    return this.allowCreate;
  }

  /**
   * Set whether the PersistenceManagerFactory proxy is allowed to create
   * a non-transactional PersistenceManager when no transactional
   * PersistenceManager can be found for the current thread.
   * <p>Default is "true". Can be turned off to enforce access to
   * transactional PersistenceManagers, which safely allows for DAOs
   * written to get a PersistenceManager without explicit closing
   * (i.e. a {@code PersistenceManagerFactory.getPersistenceManager()}
   * call without corresponding {@code PersistenceManager.close()} call).
   */
  public void setAllowCreate(boolean allowCreate) {
    this.allowCreate = allowCreate;
  }

  @Override
  public void afterPropertiesSet() {
    if (getPersistenceManagerFactory() == null) {
      throw new IllegalArgumentException("Property 'persistenceManagerFactory' is required");
    }
    // Build default JdoDialect if none explicitly specified.
    if (this.jdoDialect == null) {
      this.jdoDialect = new DefaultJdoDialect(getPersistenceManagerFactory().getConnectionFactory());
    }
    this.proxy = (PersistenceManager) Proxy.newProxyInstance(
      getPersistenceManagerFactory().getClass().getClassLoader(),
      new Class<?>[]{getPersistenceManagerInterface()}, new PersistenceManagerInvocationHandler());
  }


  @Override
  public PersistenceManager getObject() {
    return this.proxy;
  }

  @Override
  public Class<? extends PersistenceManager> getObjectType() {
    return getPersistenceManagerInterface();
  }

  @Override
  public boolean isSingleton() {
    return true;
  }


  /**
   * Invocation handler that delegates close calls on PersistenceManagers to
   * PersistenceManagerFactoryUtils for being aware of thread-bound transactions.
   */
  private class PersistenceManagerInvocationHandler implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // Invocation on PersistenceManager interface coming in...

      if (method.getName().equals("equals")) {
        // Only consider equal when proxies are identical.
        return (proxy == args[0]);
      } else if (method.getName().equals("hashCode")) {
        // Use hashCode of PersistenceManager proxy.
        return System.identityHashCode(proxy);
      } else if (method.getName().equals("toString")) {
        // Deliver toString without touching a target EntityManager.
        return "Spring PersistenceManager proxy for target factory [" + getPersistenceManagerFactory() + "]";
      } else if (method.getName().equals("getPersistenceManagerFactory")) {
        // Return PersistenceManagerFactory without creating a PersistenceManager.
        return getPersistenceManagerFactory();
      } else if (method.getName().equals("isClosed")) {
        // Proxy is always usable.
        return false;
      } else if (method.getName().equals("close")) {
        // Suppress close method.
        return null;
      }

      // Invoke method on target PersistenceManager.
      PersistenceManager pm = PersistenceManagerFactoryUtils.doGetPersistenceManager(
        getPersistenceManagerFactory(), isAllowCreate());
      try {
        Object retVal = method.invoke(pm, args);
        if (retVal instanceof Query) {
          PersistenceManagerFactoryUtils.applyTransactionTimeout(
            (Query) retVal, getPersistenceManagerFactory());
        }
        return retVal;
      } catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      } finally {
        PersistenceManagerFactoryUtils.doReleasePersistenceManager(pm, getPersistenceManagerFactory());
      }
    }
  }

}
