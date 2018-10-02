/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
