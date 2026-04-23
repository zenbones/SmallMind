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
package org.smallmind.persistence.cache.aop;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.SingleItemIterable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;

/**
 * AspectJ aspect that reacts to persist and delete operations on {@link CachedWith}-annotated DAOs
 * by updating or invalidating the declared cache vectors.
 */
@Aspect
public class CachedWithAspect {

  private static final ConcurrentHashMap<MethodKey, Method> METHOD_MAP = new ConcurrentHashMap<>();

  /**
   * Around advice applied to persist calls; updates or invalidates cache vectors after the durable is written.
   *
   * @param thisJoinPoint the intercepted persist invocation
   * @param ormDao        the DAO performing the persist operation
   * @return the persisted durable returned by the underlying method
   * @throws Throwable if the underlying method throws or a cache operation fails
   */
  @Around(value = "(execution(* persist (org.smallmind.persistence.Durable+)) || execution(@Persist * * (org.smallmind.persistence.Durable+))) && @within(CachedWith) && this(ormDao)", argNames = "thisJoinPoint, ormDao")
  public Object aroundPersistMethod (ProceedingJoinPoint thisJoinPoint, ORMDao ormDao)
    throws Throwable {

    CachedWith cachedWith;
    VectoredDao vectoredDao;

    if (((vectoredDao = ormDao.getVectoredDao()) == null) || ((cachedWith = ormDao.getClass().getAnnotation(CachedWith.class)) == null)) {

      return thisJoinPoint.proceed();
    } else {

      Durable durable;

      if ((durable = (Durable)thisJoinPoint.proceed()) == null) {

        return thisJoinPoint.proceed();
      } else {
        for (Update update : cachedWith.updates()) {
          if (executeFilter(update.filter(), ormDao, durable)) {

            OnPersist onPersist = executeOnPersist(update.onPersist(), ormDao, durable);
            Iterable<Durable> finderIterable = executeFinder(update.finder(), ormDao, durable);

            for (Durable indexingDurable : finderIterable) {

              Operand operand = executeProxy(update.proxy(), ormDao, indexingDurable);

              switch (onPersist) {
                case INSERT:
                  vectoredDao.updateInVector(new VectorKey(VectorCalculator.getVectorArtifact(update.value(), operand.getDurable()), ormDao.getManagedClass(), Classifications.get(CachedWith.class, null, update.value())), indexingDurable);
                  break;
                case REMOVE:
                  vectoredDao.removeFromVector(new VectorKey(VectorCalculator.getVectorArtifact(update.value(), operand.getDurable()), ormDao.getManagedClass(), Classifications.get(CachedWith.class, null, update.value())), indexingDurable);
                  break;
                default:
                  throw new UnknownSwitchCaseException(onPersist.name());
              }
            }
          }
        }

        for (Invalidate invalidate : cachedWith.invalidates()) {
          if (executeFilter(invalidate.filter(), ormDao, durable)) {

            Iterable<Durable> finderIterable = executeFinder(invalidate.finder(), ormDao, durable);

            for (Durable indexingDurable : finderIterable) {

              Operand operand = executeProxy(invalidate.proxy(), ormDao, indexingDurable);

              vectoredDao.deleteVector(new VectorKey(VectorCalculator.getVectorArtifact(invalidate.value(), operand.getDurable()), ormDao.getManagedClass(), Classifications.get(CachedWith.class, null, invalidate.value())));
            }
          }
        }

        return durable;
      }
    }
  }

  /**
   * Around advice applied to delete calls; removes the deleted durable from any matching vectors after deletion.
   *
   * @param thisJoinPoint the intercepted delete invocation
   * @param ormDao        the DAO performing the delete operation
   * @param durable       the durable instance being deleted
   * @throws Throwable if the underlying method throws or a cache operation fails
   */
  @Around(value = "(execution(void delete (..)) || execution(@Delete * * (..))) && @within(CachedWith) && args(durable) && this(ormDao)", argNames = "thisJoinPoint, ormDao, durable")
  public void aroundDeleteMethod (ProceedingJoinPoint thisJoinPoint, ORMDao ormDao, Durable durable)
    throws Throwable {

    CachedWith cachedWith;
    VectoredDao vectoredDao;

    if (((vectoredDao = ormDao.getVectoredDao()) == null) || ((cachedWith = ormDao.getClass().getAnnotation(CachedWith.class)) == null) || (durable == null)) {

      thisJoinPoint.proceed();
    } else {
      thisJoinPoint.proceed();

      for (Update update : cachedWith.updates()) {
        if (executeFilter(update.filter(), ormDao, durable)) {

          Iterable<Durable> finderIterable = executeFinder(update.finder(), ormDao, durable);

          for (Durable indexingDurable : finderIterable) {

            Operand operand = executeProxy(update.proxy(), ormDao, indexingDurable);

            vectoredDao.removeFromVector(new VectorKey(VectorCalculator.getVectorArtifact(update.value(), operand.getDurable()), ormDao.getManagedClass(), Classifications.get(CachedWith.class, null, update.value())), indexingDurable);
          }
        }
      }

      for (Invalidate invalidate : cachedWith.invalidates()) {
        if (executeFilter(invalidate.filter(), ormDao, durable)) {

          Iterable<Durable> finderIterable = executeFinder(invalidate.finder(), ormDao, durable);

          for (Durable indexingDurable : finderIterable) {

            Operand operand = executeProxy(invalidate.proxy(), ormDao, indexingDurable);

            vectoredDao.deleteVector(new VectorKey(VectorCalculator.getVectorArtifact(invalidate.value(), operand.getDurable()), ormDao.getManagedClass(), Classifications.get(CachedWith.class, null, invalidate.value())));
          }
        }
      }
    }
  }

  /**
   * Invokes the named filter method on the DAO to determine whether a cache operation should proceed.
   *
   * @param filterMethodName name of the boolean-returning filter method; empty string skips filtering
   * @param ormDao           the DAO that declares the filter method
   * @param durable          the durable supplied to the filter method
   * @return {@code true} if the cache operation should proceed, {@code false} to skip it
   */
  private boolean executeFilter (String filterMethodName, ORMDao ormDao, Durable durable) {

    Method filterMethod;

    if (filterMethodName.length() > 0) {

      if ((filterMethod = locateMethod(ormDao, filterMethodName, ormDao.getManagedClass())) == null) {
        throw new CacheAutomationError("The filter Method(%s) referenced within @CachedWith does not exist", filterMethodName);
      }

      if (!(filterMethod.getReturnType().equals(boolean.class) || filterMethod.getReturnType().equals(Boolean.class))) {
        throw new CacheAutomationError("A filter Method(%s) referenced by @CachedWith must return a value of type 'boolean'", filterMethodName);
      }

      try {

        return (Boolean)filterMethod.invoke(ormDao, durable);
      } catch (Exception exception) {
        throw new CacheAutomationError(exception);
      }
    }

    return true;
  }

  /**
   * Resolves the {@link OnPersist} action for a cache update, optionally delegating to a DAO method.
   *
   * @param onPersistMethodName name of a DAO method returning {@link OnPersist}; empty string defaults to {@link OnPersist#INSERT}
   * @param ormDao              the DAO that declares the strategy method
   * @param durable             the durable supplied to the strategy method
   * @return the {@link OnPersist} action to apply
   */
  private OnPersist executeOnPersist (String onPersistMethodName, ORMDao ormDao, Durable durable) {

    Method onPersistMethod;

    if (onPersistMethodName.length() > 0) {

      if ((onPersistMethod = locateMethod(ormDao, onPersistMethodName, ormDao.getManagedClass())) == null) {
        throw new CacheAutomationError("The onPersist Method(%s) referenced within @CachedWith does not exist", onPersistMethodName);
      }

      if (!onPersistMethod.getReturnType().equals(OnPersist.class)) {
        throw new CacheAutomationError("An onPersist Method(%s) referenced by @CachedWith must return a value of type 'OnPersist'", onPersistMethodName);
      }

      try {

        return (OnPersist)onPersistMethod.invoke(ormDao, durable);
      } catch (Exception exception) {
        throw new CacheAutomationError(exception);
      }
    }

    return OnPersist.INSERT;
  }

  /**
   * Applies an optional proxy transformation to a durable before it is used for cache key construction.
   *
   * @param proxy   proxy configuration that names the transformation method and expected return type
   * @param ormDao  the DAO that declares the proxy method
   * @param durable the durable to transform
   * @return an {@link Operand} holding the (possibly transformed) durable and its managed class
   */
  private Operand executeProxy (Proxy proxy, ORMDao ormDao, Durable durable) {

    Method proxyMethod;
    Class<? extends Durable> expectedType;

    if ((proxy.method() == null) || (proxy.method().length() == 0)) {

      return new Operand(ormDao.getManagedClass(), durable);
    }

    if ((proxyMethod = locateMethod(ormDao, proxy.method(), ormDao.getManagedClass())) == null) {
      throw new CacheAutomationError("The proxy method(%s) does not exist", proxy.method());
    }
    if (!(expectedType = proxy.with().equals(Durable.class) ? durable.getClass() : proxy.with()).isAssignableFrom(proxyMethod.getReturnType())) {
      throw new CacheAutomationError("The proxy method(%s) must return a %s type", proxy.method(), expectedType);
    }

    try {

      return new Operand(expectedType, (Durable)proxyMethod.invoke(ormDao, durable));
    } catch (Exception exception) {
      throw new CacheAutomationError(exception);
    }
  }

  /**
   * Invokes the configured finder to produce the durables that will be added to or removed from cache vectors.
   *
   * @param finder  finder configuration naming the DAO method and expected return type
   * @param ormDao  the DAO that declares the finder method
   * @param durable the durable supplied as the argument to the finder method
   * @return an iterable of durables to use for cache key construction
   */
  private Iterable<Durable> executeFinder (Finder finder, ORMDao ormDao, Durable durable) {

    Method finderMethod;
    Type finderReturnType;
    Class<? extends Durable> expectedType;

    if ((finder.method() == null) || (finder.method().length() == 0)) {

      return new SingleItemIterable<>(durable);
    }

    if ((finderMethod = locateMethod(ormDao, finder.method(), ormDao.getManagedClass())) == null) {
      throw new CacheAutomationError("The finder method(%s) does not exist", finder.method());
    }

    if ((expectedType = (finder.with().equals(Durable.class) ? durable.getClass() : finder.with())).isAssignableFrom(finderMethod.getReturnType())) {
      try {

        return new SingleItemIterable<>((Durable)finderMethod.invoke(ormDao, durable));
      } catch (Exception exception) {
        throw new CacheAutomationError(exception);
      }
    } else if (!Iterable.class.isAssignableFrom(finderMethod.getReturnType())) {
      if ((!((finderReturnType = finderMethod.getGenericReturnType()) instanceof ParameterizedType)) || (!expectedType.isAssignableFrom((Class)((ParameterizedType)finderReturnType).getActualTypeArguments()[0]))) {
        throw new CacheAutomationError("The finder method(%s) must return an Iterable parameterized to %s <? extends Iterable<? extends %s>>", finder.method(), expectedType.getSimpleName(), expectedType.getSimpleName());
      }

      try {

        return (Iterable<Durable>)finderMethod.invoke(ormDao, durable);
      } catch (Exception exception) {
        throw new CacheAutomationError(exception);
      }
    } else {
      throw new CacheAutomationError("The finder method(%s) must return either a %s type, or an Iterable parameterized to %s <? extends Iterable<? extends %s>>", finder.method(), expectedType.getSimpleName(), expectedType.getSimpleName(), expectedType.getSimpleName());
    }
  }

  /**
   * Looks up and caches a public, single-parameter method by name on the given DAO.
   *
   * @param ormDao        the DAO whose public methods are searched
   * @param methodName    the method name to match
   * @param parameterType the type the method's sole parameter must be assignable from
   * @return the matching {@link Method}, or {@code null} if none is found
   */
  private Method locateMethod (ORMDao ormDao, String methodName, Class parameterType) {

    Method aspectMethod;
    MethodKey methodKey;
    Class[] parameterTypes;

    if ((aspectMethod = METHOD_MAP.get(methodKey = new MethodKey(ormDao.getClass(), methodName))) == null) {
      for (Method method : ormDao.getClass().getMethods()) {
        if (method.getName().equals(methodName) && ((parameterTypes = method.getParameterTypes()).length == 1) && (parameterTypes[0].isAssignableFrom(parameterType))) {
          METHOD_MAP.put(methodKey, aspectMethod = method);
          break;
        }
      }
    }

    return aspectMethod;
  }

  /**
   * Pairs a durable instance with its managed class, as produced by a proxy transformation.
   */
  public class Operand {

    private final Class<? extends Durable> managedClass;
    private final Durable durable;

    /**
     * Constructs an operand from a managed class and the corresponding durable.
     *
     * @param managedClass the durable class that governs cache key construction
     * @param durable      the durable instance to act on
     */
    private Operand (Class<? extends Durable> managedClass, Durable durable) {

      this.managedClass = managedClass;
      this.durable = durable;
    }

    /**
     * Returns the managed durable class for this operand.
     *
     * @return managed class used for vector key construction
     */
    public Class<? extends Durable> getManagedClass () {

      return managedClass;
    }

    /**
     * Returns the durable instance to insert into or remove from a cache vector.
     *
     * @return durable entity for the cache operation
     */
    public Durable getDurable () {

      return durable;
    }
  }

  /**
   * Composite cache key that identifies a resolved {@link Method} by its declaring class and name.
   */
  public class MethodKey {

    private final Class methodClass;
    private final String methodName;

    /**
     * Constructs a method key from a class and method name.
     *
     * @param methodClass the class that declares the method
     * @param methodName  the simple method name
     */
    private MethodKey (Class methodClass, String methodName) {

      this.methodClass = methodClass;
      this.methodName = methodName;
    }

    /**
     * Returns the class that declares the cached method.
     *
     * @return declaring class of the method
     */
    public Class getMethodClass () {

      return methodClass;
    }

    /**
     * Returns the simple name of the cached method.
     *
     * @return method name
     */
    public String getMethodName () {

      return methodName;
    }

    /**
     * Combines the declaring class and method name into a stable hash code.
     *
     * @return XOR of the class and method-name hash codes
     */
    @Override
    public int hashCode () {

      return methodClass.hashCode() ^ methodName.hashCode();
    }

    /**
     * Two keys are equal when they share the same declaring class and method name.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is a {@link MethodKey} with identical class and name
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof MethodKey) && methodClass.equals(((MethodKey)obj).getMethodClass()) && methodName.equals(((MethodKey)obj).getMethodName());
    }
  }
}
