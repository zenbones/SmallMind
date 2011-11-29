package org.smallmind.persistence.cache.memcached.spring;

import java.io.IOException;
import java.util.Map;
import net.rubyeye.xmemcached.MemcachedClient;
import org.smallmind.persistence.cache.memcached.MemcachedCacheDomain;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class MemcachedCacheDomainFactoryBean implements FactoryBean<MemcachedCacheDomain>, InitializingBean {

  private MemcachedCacheDomain memcachedCacheDomain;
  private MemcachedClient memcachedClient;
  private Map<Class, Integer> timeToLiveOverrideMap;
  private int timeToLiveSeconds;

  public void setMemcachedClient (MemcachedClient memcachedClient) {

    this.memcachedClient = memcachedClient;
  }

  public void setTimeToLiveSeconds (int timeToLiveSeconds) {

    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  public void setTimeToLiveOverrideMap (Map<Class, Integer> timeToLiveOverrideMap) {

    this.timeToLiveOverrideMap = timeToLiveOverrideMap;
  }

  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (memcachedClient != null) {
      memcachedCacheDomain = new MemcachedCacheDomain(memcachedClient, timeToLiveSeconds, timeToLiveOverrideMap);
    }
  }

  @Override
  public MemcachedCacheDomain getObject () {

    return memcachedCacheDomain;
  }

  @Override
  public Class<?> getObjectType () {

    return MemcachedCacheDomain.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}
