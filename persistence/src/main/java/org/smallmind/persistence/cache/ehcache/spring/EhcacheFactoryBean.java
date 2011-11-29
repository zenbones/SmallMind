package org.smallmind.persistence.cache.ehcache.spring;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.smallmind.persistence.cache.ehcache.HeapMemory;
import org.smallmind.persistence.cache.ehcache.MemoryUnit;
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
  private boolean eternal = false;
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

  public void setEternal (boolean eternal) {

    this.eternal = eternal;
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

    configuration = new CacheConfiguration(name, maxElementsInMemory)
      .overflowToDisk(false).eternal(eternal)
      .timeToIdleSeconds(timeToIdleSeconds)
      .timeToLiveSeconds(timeToLiveSeconds)
      .memoryStoreEvictionPolicy(memoryStoreEvictionPolicy)
      .maxElementsOnDisk(maxElementsOnDisk).overflowToOffHeap(overflowToOffHeap).diskPersistent(diskPersistent)
      .maxMemoryOffHeap(maxMemoryOffHeap.toString())
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
      configuration.terracotta(new TerracottaConfiguration().clustered(true).consistency(TerracottaConfiguration.Consistency.STRONG).valueMode(terracottaMode).storageStrategy(TerracottaConfiguration.StorageStrategy.CLASSIC));
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
