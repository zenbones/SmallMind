package org.smallmind.persistence.cache.memcached.spring;

import java.util.LinkedList;
import org.smallmind.persistence.cache.memcached.MemcachedServer;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class MemcachedServerFactoryBean implements FactoryBean<MemcachedServer[]>, InitializingBean {

  private MemcachedServer[] serverArray;
  private String servers;

  public void setServers (String servers) {

    this.servers = servers;
  }

  @Override
  public void afterPropertiesSet () {

    if ((servers != null) && (servers.length() > 0)) {

      LinkedList<MemcachedServer> serverList;
      int colonPos;

      serverList = new LinkedList<MemcachedServer>();
      for (String server : servers.split(",", -1)) {
        if ((colonPos = server.indexOf(':')) >= 0) {
          serverList.add(new MemcachedServer(server.substring(0, colonPos), Integer.parseInt(server.substring(colonPos + 1))));
        }
        else {
          serverList.add(new MemcachedServer(server, 11211));
        }
      }

      serverArray = new MemcachedServer[serverList.size()];
      serverList.toArray(serverArray);
    }
  }

  @Override
  public MemcachedServer[] getObject () {

    return serverArray;
  }

  @Override
  public Class<?> getObjectType () {

    return MemcachedServer[].class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}
