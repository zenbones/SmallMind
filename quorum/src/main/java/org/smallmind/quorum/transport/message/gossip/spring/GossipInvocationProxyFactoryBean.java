/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.transport.message.gossip.spring;

import java.lang.reflect.Proxy;
import org.smallmind.quorum.transport.message.gossip.GossipInvocationProxyFactory;
import org.smallmind.quorum.transport.message.gossip.GossipTransmitter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class GossipInvocationProxyFactoryBean implements InitializingBean, FactoryBean<Proxy> {

  private Class<?> serviceInterface;
  private GossipTransmitter gossipTransmitter;
  private Proxy serviceProxy;

  public void setServiceInterface (Class<?> serviceInterface) {

    this.serviceInterface = serviceInterface;
  }

  public void setGossipTransmitter (GossipTransmitter gossipTransmitter) {

    this.gossipTransmitter = gossipTransmitter;
  }

  @Override
  public void afterPropertiesSet ()
    throws Exception {

    serviceProxy = GossipInvocationProxyFactory.generateProxy(gossipTransmitter, serviceInterface);
  }

  @Override
  public Proxy getObject () throws Exception {

    return serviceProxy;
  }

  @Override
  public Class<?> getObjectType () {

    return serviceInterface;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}
