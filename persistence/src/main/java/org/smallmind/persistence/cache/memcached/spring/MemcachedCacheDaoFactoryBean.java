package org.smallmind.persistence.cache.memcached.spring;

import java.io.IOException;
import org.smallmind.persistence.cache.memcached.MemcachedCacheDomain;
import org.smallmind.persistence.cache.praxis.extrinsic.ByKeyExtrinsicCacheDao;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class MemcachedCacheDaoFactoryBean implements FactoryBean<ByKeyExtrinsicCacheDao>, InitializingBean {

  private ByKeyExtrinsicCacheDao memcachedCacheDao;
  private MemcachedCacheDomain memcachedCacheDomain;

  public void setMemcachedCacheDomain (MemcachedCacheDomain memcachedCacheDomain) {

    this.memcachedCacheDomain = memcachedCacheDomain;
  }

  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (memcachedCacheDomain != null) {
      memcachedCacheDao = new ByKeyExtrinsicCacheDao(memcachedCacheDomain);
    }
  }

  @Override
  public ByKeyExtrinsicCacheDao getObject () {

    return memcachedCacheDao;
  }

  @Override
  public Class<?> getObjectType () {

    return ByKeyExtrinsicCacheDao.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}