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
package org.smallmind.memcached.spring;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.aws.AWSElasticCacheClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import org.smallmind.memcached.MemcachedServer;
import org.smallmind.memcached.XMemcachedMemcachedClient;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class AWSElasticCacheMemcachedClientFactoryBean implements FactoryBean<XMemcachedMemcachedClient>, InitializingBean {

  private XMemcachedMemcachedClient memcachedClient;
  private Transcoder<?> transcoder;
  private MemcachedServer[] servers;
  private long pollIntervalMilliseconds = 30000;
  private boolean enabled = true;
  private int poolSize;

  public void setEnabled (boolean enabled) {

    this.enabled = enabled;
  }

  public void setMemcachedClient (XMemcachedMemcachedClient memcachedClient) {

    this.memcachedClient = memcachedClient;
  }

  public void setServers (MemcachedServer[] servers) {

    this.servers = servers;
  }

  public void setPoolSize (int poolSize) {

    this.poolSize = poolSize;
  }

  public void setPollIntervalMilliseconds (long pollIntervalMilliseconds) {

    this.pollIntervalMilliseconds = pollIntervalMilliseconds;
  }

  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (enabled && (servers != null) && (servers.length > 0)) {

      MemcachedClientBuilder builder;
      LinkedList<InetSocketAddress> addressList;

      addressList = new LinkedList<>();
      for (MemcachedServer server : servers) {
        addressList.add(new InetSocketAddress(server.getHost(), server.getPort()));
      }

      builder = new AWSElasticCacheClientBuilder(addressList);

      if (transcoder != null) {
        builder.setTranscoder(transcoder);
      }

      builder.setConnectionPoolSize(poolSize);
      builder.setCommandFactory(new BinaryCommandFactory());
      builder.setSessionLocator(new KetamaMemcachedSessionLocator());

      memcachedClient = new XMemcachedMemcachedClient(builder.build());
    }
  }

  @Override
  public XMemcachedMemcachedClient getObject () {

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

    if (memcachedClient != null) {
      try {
        memcachedClient.shutdown();
      } catch (IOException ioException) {
        LoggerManager.getLogger(AWSElasticCacheMemcachedClientFactoryBean.class).error(ioException);
      }
    }
  }
}