package org.smallmind.persistence.cache.memcached.spring;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import org.smallmind.persistence.cache.memcached.MemcachedServer;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class MemcachedClientFactoryBean implements FactoryBean<MemcachedClient>, InitializingBean {

  private MemcachedClient memcachedClient;
  private MemcachedServer[] servers;
  private boolean enabled = true;
  private int poolSize;

  public void setEnabled (boolean enabled) {

    this.enabled = enabled;
  }

  public void setServers (MemcachedServer[] servers) {

    this.servers = servers;
  }

  public void setPoolSize (int poolSize) {

    this.poolSize = poolSize;
  }

  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (enabled && (servers != null) && (servers.length > 0)) {

      MemcachedClientBuilder builder;
      LinkedList<InetSocketAddress> addressList;

      addressList = new LinkedList<InetSocketAddress>();
      for (MemcachedServer server : servers) {
        addressList.add(new InetSocketAddress(server.getHost(), server.getPort()));
      }

      builder = new XMemcachedClientBuilder(addressList);
      builder.setFailureMode(true);
      builder.setConnectionPoolSize(poolSize);
      builder.setCommandFactory(new BinaryCommandFactory());
      builder.setSessionLocator(new KetamaMemcachedSessionLocator());
      memcachedClient = builder.build();
    }
  }

  @Override
  public MemcachedClient getObject () {

    return memcachedClient;
  }

  @Override
  public Class<?> getObjectType () {

    return MemcachedClient.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  public void shutdown () {

    if (memcachedClient != null)
      try {
        memcachedClient.shutdown();
      }
      catch (IOException e) {
        LoggerManager.getLogger(MemcachedClientFactoryBean.class).error(e);
      }
  }
}