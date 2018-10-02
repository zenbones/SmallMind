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
package org.smallmind.persistence.orm.spring.jdo.antique;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

public class TransactionAwarePersistenceManagerFactoryProxy implements FactoryBean<PersistenceManagerFactory> {

  private PersistenceManagerFactory target;

  private boolean allowCreate = true;

  private PersistenceManagerFactory proxy;

  /**
   * Return the target JDO PersistenceManagerFactory that this proxy delegates to.
   */
  public PersistenceManagerFactory getTargetPersistenceManagerFactory () {

    return this.target;
  }

  /**
   * Set the target JDO PersistenceManagerFactory that this proxy should
   * delegate to. This should be the raw PersistenceManagerFactory, as
   * accessed by JdoTransactionManager.
   */
  public void setTargetPersistenceManagerFactory (PersistenceManagerFactory target) {

    Assert.notNull(target, "Target PersistenceManagerFactory must not be null");
    this.target = target;
    Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(target.getClass(), target.getClass().getClassLoader());
    this.proxy = (PersistenceManagerFactory)Proxy.newProxyInstance(
      target.getClass().getClassLoader(), ifcs, new PersistenceManagerFactoryInvocationHandler());
  }

  /**
   * Return whether the PersistenceManagerFactory proxy is allowed to create
   * a non-transactional PersistenceManager when no transactional
   * PersistenceManager can be found for the current thread.
   */
  protected boolean isAllowCreate () {

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
  public void setAllowCreate (boolean allowCreate) {

    this.allowCreate = allowCreate;
  }

  @Override
  public PersistenceManagerFactory getObject () {

    return this.proxy;
  }

  @Override
  public Class<? extends PersistenceManagerFactory> getObjectType () {

    return PersistenceManagerFactory.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Invocation handler that delegates getPersistenceManager calls on the
   * PersistenceManagerFactory proxy to PersistenceManagerFactoryUtils
   * for being aware of thread-bound transactions.
   */
  private class PersistenceManagerFactoryInvocationHandler implements InvocationHandler {

    @Override
    public Object invoke (Object proxy, Method method, Object[] args) throws Throwable {
      // Invocation on PersistenceManagerFactory interface coming in...

      if (method.getName().equals("equals")) {
        // Only consider equal when proxies are identical.
        return (proxy == args[0]);
      } else if (method.getName().equals("hashCode")) {
        // Use hashCode of PersistenceManagerFactory proxy.
        return System.identityHashCode(proxy);
      } else if (method.getName().equals("getPersistenceManager")) {
        PersistenceManagerFactory target = getTargetPersistenceManagerFactory();
        PersistenceManager pm =
          PersistenceManagerFactoryUtils.doGetPersistenceManager(target, isAllowCreate());
        Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(pm.getClass(), pm.getClass().getClassLoader());
        return Proxy.newProxyInstance(
          pm.getClass().getClassLoader(), ifcs, new PersistenceManagerInvocationHandler(pm, target));
      }

      // Invoke method on target PersistenceManagerFactory.
      try {
        return method.invoke(getTargetPersistenceManagerFactory(), args);
      } catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }
  }

  /**
   * Invocation handler that delegates close calls on PersistenceManagers to
   * PersistenceManagerFactoryUtils for being aware of thread-bound transactions.
   */
  private static class PersistenceManagerInvocationHandler implements InvocationHandler {

    private final PersistenceManager target;

    private final PersistenceManagerFactory persistenceManagerFactory;

    public PersistenceManagerInvocationHandler (PersistenceManager target, PersistenceManagerFactory pmf) {

      this.target = target;
      this.persistenceManagerFactory = pmf;
    }

    @Override
    public Object invoke (Object proxy, Method method, Object[] args) throws Throwable {
      // Invocation on PersistenceManager interface coming in...

      if (method.getName().equals("equals")) {
        // Only consider equal when proxies are identical.
        return (proxy == args[0]);
      } else if (method.getName().equals("hashCode")) {
        // Use hashCode of PersistenceManager proxy.
        return System.identityHashCode(proxy);
      } else if (method.getName().equals("close")) {
        // Handle close method: only close if not within a transaction.
        PersistenceManagerFactoryUtils.doReleasePersistenceManager(
          this.target, this.persistenceManagerFactory);
        return null;
      }

      // Invoke method on target PersistenceManager.
      try {
        return method.invoke(this.target, args);
      } catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }
  }
}
