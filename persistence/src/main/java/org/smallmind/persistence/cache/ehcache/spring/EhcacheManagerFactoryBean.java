package org.smallmind.persistence.cache.ehcache.spring;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class EhcacheManagerFactoryBean implements FactoryBean<CacheManager>, InitializingBean {

  private CacheManager cacheManager;

  @Override
  public void afterPropertiesSet () throws Exception {

    Configuration configuration = new Configuration().defaultCache(new CacheConfiguration("defaultCache", 100));

    cacheManager = new CacheManager(configuration);
  }

  @Override
  public CacheManager getObject () {

    return cacheManager;
  }

  @Override
  public Class<?> getObjectType () {

    return CacheManager.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}
