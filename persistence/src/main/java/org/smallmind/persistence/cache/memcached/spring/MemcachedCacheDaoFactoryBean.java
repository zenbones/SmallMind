package org.smallmind.persistence.cache.memcached.spring;

import java.io.IOException;
import org.smallmind.persistence.cache.memcached.MemcachedCacheDomain;
import org.smallmind.persistence.cache.praxis.distributed.ByKeyDistributedCacheDao;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class MemcachedCacheDaoFactoryBean implements FactoryBean<ByKeyDistributedCacheDao>, InitializingBean {

  private ByKeyDistributedCacheDao memcachedCacheDao;
  private MemcachedCacheDomain memcachedCacheDomain;

  public void setMemcachedCacheDomain (MemcachedCacheDomain memcachedCacheDomain) {

    this.memcachedCacheDomain = memcachedCacheDomain;
  }

  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (memcachedCacheDomain != null) {
      memcachedCacheDao = new ByKeyDistributedCacheDao(memcachedCacheDomain);
    }
  }

  @Override
  public ByKeyDistributedCacheDao getObject () {

    return memcachedCacheDao;
  }

  @Override
  public Class<?> getObjectType () {

    return ByKeyDistributedCacheDao.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}