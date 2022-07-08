package org.smallmind.memcached.cubby.spring;

import java.util.HashMap;
import java.util.Map;
import org.smallmind.memcached.utility.MemcachedServer;
import org.smallmind.nutsnbolts.util.Spread;
import org.smallmind.nutsnbolts.util.SpreadParserException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class MemcachedServerMapFactoryBean implements FactoryBean<Map<String, MemcachedServer>>, InitializingBean {

  private Map<String, MemcachedServer> serverMap;
  private String serverPattern;
  private String serverSpread;

  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

  @Override
  public void afterPropertiesSet ()
    throws SpreadParserException {

    serverMap = new HashMap<>();

    if ((serverPattern != null) && (serverPattern.length() > 0)) {

      int colonPos = serverPattern.indexOf(':');
      int poundPos;

      if ((poundPos = serverPattern.indexOf('#')) < 0) {
        if (colonPos >= 0) {
          serverMap.put("memcached", new MemcachedServer(serverPattern.substring(0, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
        } else {
          serverMap.put("memcached", new MemcachedServer(serverPattern, 11211));
        }
      } else {
        for (String serverDesignator : Spread.calculate(serverSpread)) {
          if (colonPos >= 0) {
            serverMap.put(serverDesignator, new MemcachedServer(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
          } else {
            serverMap.put(serverDesignator, new MemcachedServer(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1), 11211));
          }
        }
      }
    }
  }

  @Override
  public Map<String, MemcachedServer> getObject () {

    return serverMap;
  }

  @Override
  public Class<?> getObjectType () {

    return Map.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}
