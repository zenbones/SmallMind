/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.persistence.cache.ehcache.spring;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.smallmind.persistence.cache.ehcache.HeapMemory;
import org.smallmind.persistence.cache.ehcache.TransactionalMode;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class EhcacheFactoryBean implements FactoryBean<Cache>, InitializingBean {

  private Cache cache;
  private CacheManager cacheManager;
  private TerracottaConfiguration.ValueMode terracottaMode;
  private MemoryStoreEvictionPolicy memoryStoreEvictionPolicy = MemoryStoreEvictionPolicy.LRU;
  private TransactionalMode transactionalMode = TransactionalMode.OFF;
  private HeapMemory maxMemoryOffHeap = new HeapMemory(0, MemoryUnit.MEGABYTES);
  private String name;
  private long timeToIdleSeconds = 0;
  private long timeToLiveSeconds = 0;
  private int maxElementsInMemory = Integer.MAX_VALUE;
  private int maxElementsOnDisk = 0;
  private boolean overflowToOffHeap = false;
  private boolean diskPersistent = false;
  private boolean statistics = false;
  private boolean logging = true;
  private boolean copyOnRead = false;
  private boolean copyOnWrite = false;

  public EhcacheFactoryBean (CacheManager cacheManager, String name) {

    this.cacheManager = cacheManager;
    this.name = name;
  }

  public void setMaxElementsInMemory (int maxElementsInMemory) {

    this.maxElementsInMemory = maxElementsInMemory;
  }

  public void setTimeToIdleSeconds (long timeToIdleSeconds) {

    this.timeToIdleSeconds = timeToIdleSeconds;
  }

  public void setTimeToLiveSeconds (long timeToLiveSeconds) {

    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  public void setMemoryStoreEvictionPolicy (MemoryStoreEvictionPolicy memoryStoreEvictionPolicy) {

    this.memoryStoreEvictionPolicy = memoryStoreEvictionPolicy;
  }

  public void setMaxElementsOnDisk (int maxElementsOnDisk) {

    this.maxElementsOnDisk = maxElementsOnDisk;
  }

  public void setOverflowToOffHeap (boolean overflowToOffHeap) {

    this.overflowToOffHeap = overflowToOffHeap;
  }

  public boolean isDiskPersistent () {

    return diskPersistent;
  }

  public void setDiskPersistent (boolean diskPersistent) {

    this.diskPersistent = diskPersistent;
  }

  public void setMaxMemoryOffHeap (HeapMemory maxMemoryOffHeap) {

    this.maxMemoryOffHeap = maxMemoryOffHeap;
  }

  public void setTransactionalMode (TransactionalMode transactionalMode) {

    this.transactionalMode = transactionalMode;
  }

  public void setStatistics (boolean statistics) {

    this.statistics = statistics;
  }

  public void setLogging (boolean logging) {

    this.logging = logging;
  }

  public void setCopyOnRead (boolean copyOnRead) {

    this.copyOnRead = copyOnRead;
  }

  public void setCopyOnWrite (boolean copyOnWrite) {

    this.copyOnWrite = copyOnWrite;
  }

  public void setTerracottaMode (TerracottaConfiguration.ValueMode terracottaMode) {

    this.terracottaMode = terracottaMode;
  }

  @Override
  public void afterPropertiesSet () throws Exception {

    if (name == null) {
      throw new IllegalArgumentException("Every cache deserves a name");
    }

    CacheConfiguration configuration;
    net.sf.ehcache.config.MemoryUnit l;

    configuration = new CacheConfiguration(name, maxElementsInMemory)
      .timeToIdleSeconds(timeToIdleSeconds)
      .timeToLiveSeconds(timeToLiveSeconds)
      .memoryStoreEvictionPolicy(memoryStoreEvictionPolicy)
      .maxElementsOnDisk(maxElementsOnDisk).overflowToOffHeap(overflowToOffHeap).diskPersistent(diskPersistent)
      .maxBytesLocalOffHeap(maxMemoryOffHeap.getSize(), maxMemoryOffHeap.getUnit())
      .statistics(statistics)
      .logging(logging)
      .copyOnRead(copyOnRead)
      .copyOnWrite(copyOnWrite);

    if (transactionalMode.equals(TransactionalMode.LOCAL)) {
      configuration.transactionalMode(transactionalMode.asString());
    }
    else {
      configuration.transactionalMode(transactionalMode.asConfiguration());
    }

    if (terracottaMode != null) {
      configuration.terracotta(new TerracottaConfiguration().clustered(true).consistency(TerracottaConfiguration.Consistency.STRONG).valueMode(terracottaMode));
    }

    cacheManager.addCache(cache = new Cache(configuration));
  }

  @Override
  public Cache getObject () {

    return cache;
  }

  @Override
  public Class<?> getObjectType () {

    return Cache.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}
