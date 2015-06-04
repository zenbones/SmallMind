package org.smallmind.phalanx.wire.jms.hornetq.spring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.smallmind.nutsnbolts.spring.RuntimeBeansException;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorManager;
import org.smallmind.nutsnbolts.util.Option;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class DynamicHornetQTransportConfigurationListFactoryBean implements InitializingBean, FactoryBean<List<TransportConfiguration>> {

  /*
  jms.hornetq.transport.host.<transport name> (required for each transport)
  jms.hornetq.transport.port.<transport name> (required for each transport)
   */

  private final HashMap<String, TransportConfiguration> transportConfigurationMap = new HashMap<>();

  @Override
  public void afterPropertiesSet () {

    SpringPropertyAccessor springPropertyAccessor = SpringPropertyAccessorManager.getSpringPropertyAccessor();

    for (String key : springPropertyAccessor.getKeySet()) {

      Option<Integer> transportPortOption;
      String transportName;

      if (key.startsWith("jms.hornetq.transport.host.") && (!(transportName = key.substring("jms.hornetq.transport.host.".length())).contains("."))) {
        if ((transportPortOption = springPropertyAccessor.asInt("jms.hornetq.transport.port." + transportName)).isNone()) {
          throw new RuntimeBeansException("Missing port definition for transport configuration(%s)", transportName);
        }

        HashMap<String, Object> propertyMap = new HashMap<>();

        propertyMap.put("host", springPropertyAccessor.asString("jms.hornetq.transport.host." + transportName));
        propertyMap.put("port", transportPortOption.get().toString());
        transportConfigurationMap.put(transportName, new TransportConfiguration(NettyConnectorFactory.class.getName(), propertyMap));
      }
    }
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return List.class;
  }

  @Override
  public List<TransportConfiguration> getObject () throws Exception {

    return new LinkedList<>(transportConfigurationMap.values());
  }
}
