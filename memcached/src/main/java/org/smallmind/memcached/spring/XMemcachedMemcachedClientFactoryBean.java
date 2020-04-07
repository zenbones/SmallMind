/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import java.util.HashMap;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import org.smallmind.memcached.MemcachedServer;
import org.smallmind.memcached.XMemcachedMemcachedClient;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class XMemcachedMemcachedClientFactoryBean implements FactoryBean<XMemcachedMemcachedClient>, InitializingBean {

  private XMemcachedMemcachedClient memcachedClient;
  private Transcoder<?> transcoder;
  private MemcachedServer[] servers;
  private MemcachedServer[] backups;
  private boolean enabled = true;
  private int poolSize;

  public void setEnabled (boolean enabled) {

    this.enabled = enabled;
  }

  public void setTranscoder (Transcoder<?> transcoder) {

    this.transcoder = transcoder;
  }

  public void setServers (MemcachedServer[] servers) {

    this.servers = servers;
  }

  public void setBackups (MemcachedServer[] backups) {

    this.backups = backups;
  }

  public void setPoolSize (int poolSize) {

    this.poolSize = poolSize;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return XMemcachedMemcachedClient.class;
  }

  @Override
  public XMemcachedMemcachedClient getObject () {

    return memcachedClient;
  }

  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (enabled && (servers != null) && (servers.length > 0)) {
      if ((backups != null) && (servers.length != backups.length)) {
        throw new BeanCreationException("Must use an equal number of primary and backup servers");
      } else {

        MemcachedClientBuilder builder;
        HashMap<InetSocketAddress, InetSocketAddress> addressMap = new HashMap<>();
        int index = 0;

        for (MemcachedServer server : servers) {

          MemcachedServer backup = null;

          if (backups != null) {
            backup = backups[index];
          } else if (servers.length > 1) {
            backup = servers[index == (servers.length - 1) ? 0 : index + 1];
          }

          addressMap.put(new InetSocketAddress(server.getHost(), server.getPort()), (backup == null) ? null : new InetSocketAddress(backup.getHost(), backup.getPort()));
          index++;
        }

        builder = new XMemcachedClientBuilder(addressMap);

        if (transcoder != null) {
          builder.setTranscoder(transcoder);
        }

        builder.setFailureMode(true);
        builder.setConnectionPoolSize(poolSize);
        builder.setCommandFactory(new BinaryCommandFactory());
        builder.setSessionLocator(new KetamaMemcachedSessionLocator());

        memcachedClient = new XMemcachedMemcachedClient(builder.build());
      }
    }
  }

  public void shutdown () {

    if (memcachedClient != null) {
      try {
        memcachedClient.shutdown();
      } catch (IOException ioException) {
        LoggerManager.getLogger(XMemcachedMemcachedClientFactoryBean.class).error(ioException);
      }
    }
  }
}