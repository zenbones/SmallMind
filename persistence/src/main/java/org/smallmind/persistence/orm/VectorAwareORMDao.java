/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.persistence.orm;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.VectorAware;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.cache.aop.CacheAs;

public abstract class VectorAwareORMDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractORMDao<I, D> implements VectorAware<I, D> {

  private static final ConcurrentHashMap<MethodKey, CacheAs> ANNOTATION_MAP = new ConcurrentHashMap<MethodKey, CacheAs>();

  private ProxySession proxySession;
  private VectoredDao<I, D> vectoredDao;

  public VectorAwareORMDao (ProxySession proxySession, VectoredDao<I, D> vectoredDao) {

    this.proxySession = proxySession;
    this.vectoredDao = vectoredDao;
  }

  @Override
  public VectoredDao<I, D> getVectoredDao () {

    return proxySession.isCacheEnabled() ? vectoredDao : null;
  }

  public void purge (D durable, String methodName, Class<?>... methodSignature)
    throws NoSuchMethodException {

    VectoredDao<I, D> vectoredDao;

    if ((vectoredDao = getVectoredDao()) != null) {

      CacheAs cacheAs;
      MethodKey methodKey = new MethodKey(methodName, methodSignature);

      if ((cacheAs = ANNOTATION_MAP.get(methodKey)) == null) {
        if ((cacheAs = this.getClass().getMethod(methodName, methodSignature).getAnnotation(CacheAs.class)) == null) {
          throw new ORMOperationException("The method(%s) has no %s annotation", methodName, CacheAs.class.getSimpleName());
        }
        ANNOTATION_MAP.put(methodKey, cacheAs);
      }

      vectoredDao.deleteVector(new VectorKey<D>(cacheAs.value(), durable, getManagedClass()));
    }
  }

  public static class MethodKey {

    private Class[] methodSignature;
    private String methodName;

    private MethodKey (String methodName, Class<?>... methodSignature) {

      this.methodName = methodName;
      this.methodSignature = methodSignature;
    }

    public String getMethodName () {

      return methodName;
    }

    public Class[] getMethodSignature () {

      return methodSignature;
    }

    @Override
    public int hashCode () {

      return methodName.hashCode() ^ ((methodSignature == null) ? 0 : Arrays.hashCode(methodSignature));
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof MethodKey) && methodName.equals(((MethodKey)obj).getMethodName()) && (methodSignature == null) ? (((MethodKey)obj).getMethodSignature() == null) : Arrays.equals(methodSignature, ((MethodKey)obj).getMethodSignature());
    }
  }
}
