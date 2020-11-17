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
package org.smallmind.persistence.nosql.spring.hector;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import me.prettyprint.cassandra.connection.LoadBalancingPolicy;
import me.prettyprint.cassandra.connection.RoundRobinBalancingPolicy;
import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftCluster;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import org.smallmind.nutsnbolts.util.Spread;
import org.smallmind.nutsnbolts.util.SpreadParserException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class KeyspaceFactoryBean implements FactoryBean<Keyspace>, InitializingBean {

  private static final HashMap<String, ThriftCluster> CLUSTER_MAP = new HashMap<String, ThriftCluster>();
  private static final HashMap<ConsistencyPair, ConfigurableConsistencyLevel> CONSISTENCY_LEVEL_MAP = new HashMap<ConsistencyPair, ConfigurableConsistencyLevel>();
  private Keyspace keyspace;
  private HConsistencyLevel defaultReadConsistencyLevel = HConsistencyLevel.QUORUM;
  private HConsistencyLevel defaultWriteConsistencyLevel = HConsistencyLevel.QUORUM;
  private LoadBalancingPolicy loadBalancingPolicy = new RoundRobinBalancingPolicy();
  private String serverPattern;
  private String serverSpread;
  private String clusterName;
  private String keyspaceName;
  private String replicationStrategyClass;
  private boolean verify = false;
  private long waitTimeWhenExhausted = 5000;
  private int replicationFactor;
  private int maxActive = 50;

  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

  public void setClusterName (String clusterName) {

    this.clusterName = clusterName;
  }

  public void setKeyspaceName (String keyspaceName) {

    this.keyspaceName = keyspaceName;
  }

  public void setDefaultReadConsistencyLevel (HConsistencyLevel defaultReadConsistencyLevel) {

    this.defaultReadConsistencyLevel = defaultReadConsistencyLevel;
  }

  public void setDefaultWriteConsistencyLevel (HConsistencyLevel defaultWriteConsistencyLevel) {

    this.defaultWriteConsistencyLevel = defaultWriteConsistencyLevel;
  }

  public void setLoadBalancingPolicy (LoadBalancingPolicy loadBalancingPolicy) {

    this.loadBalancingPolicy = loadBalancingPolicy;
  }

  public void setReplicationStrategyClass (String replicationStrategyClass) {

    this.replicationStrategyClass = replicationStrategyClass;
  }

  public void setReplicationFactor (int replicationFactor) {

    this.replicationFactor = replicationFactor;
  }

  public void setMaxActive (int maxActive) {

    this.maxActive = maxActive;
  }

  public void setWaitTimeWhenExhausted (long waitTimeWhenExhausted) {

    this.waitTimeWhenExhausted = waitTimeWhenExhausted;
  }

  public void setVerify (boolean verify) {

    this.verify = verify;
  }

  @Override
  public synchronized void afterPropertiesSet ()
    throws SpreadParserException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    if ((serverPattern != null) && (serverPattern.length() > 0)) {

      ThriftCluster thriftCluster;
      ConfigurableConsistencyLevel consistencyLevelPolicy;
      ConsistencyPair consistencyPair = new ConsistencyPair(defaultReadConsistencyLevel, defaultWriteConsistencyLevel);
      StringBuilder serverBuilder = new StringBuilder();
      int poundPos;

      if ((poundPos = serverPattern.indexOf('#')) < 0) {
        serverBuilder.append(serverPattern);
      } else {

        boolean first = true;

        for (String serverDesignator : Spread.calculate(serverSpread)) {
          if (!first) {
            serverBuilder.append(',');
          }
          serverBuilder.append(serverPattern, 0, poundPos).append(serverDesignator).append(serverPattern.substring(poundPos + 1));
          first = false;
        }
      }

      if ((thriftCluster = CLUSTER_MAP.get(serverBuilder.toString())) == null) {

        CassandraHostConfigurator cassandraHostConfigurator = new CassandraHostConfigurator(serverBuilder.toString());

        cassandraHostConfigurator.setLoadBalancingPolicy(loadBalancingPolicy);
        cassandraHostConfigurator.setMaxActive(maxActive);
        cassandraHostConfigurator.setMaxWaitTimeWhenExhausted(waitTimeWhenExhausted);

        CLUSTER_MAP.put(serverBuilder.toString(), thriftCluster = new ThriftCluster(clusterName, cassandraHostConfigurator));
      }

      if ((consistencyLevelPolicy = CONSISTENCY_LEVEL_MAP.get(consistencyPair)) == null) {
        CONSISTENCY_LEVEL_MAP.put(consistencyPair, consistencyLevelPolicy = new ConfigurableConsistencyLevel());
        consistencyLevelPolicy.setDefaultReadConsistencyLevel(defaultReadConsistencyLevel);
        consistencyLevelPolicy.setDefaultWriteConsistencyLevel(defaultWriteConsistencyLevel);
      }

      keyspace = HFactory.createKeyspace(keyspaceName, thriftCluster, consistencyLevelPolicy);

      if (verify) {
        HectorSchemaVerifier.verify(thriftCluster, keyspace, replicationStrategyClass, replicationFactor);
      }
    }
  }

  @Override
  public Class<?> getObjectType () {

    return Keyspace.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Keyspace getObject () {

    return keyspace;
  }

  private class ConsistencyPair {

    private final HConsistencyLevel read;
    private final HConsistencyLevel write;

    public ConsistencyPair (HConsistencyLevel read, HConsistencyLevel write) {

      this.read = read;
      this.write = write;
    }

    public HConsistencyLevel getRead () {

      return read;
    }

    public HConsistencyLevel getWrite () {

      return write;
    }

    @Override
    public int hashCode () {

      return read.hashCode() ^ write.hashCode();
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof ConsistencyPair) && ((ConsistencyPair)obj).getRead().equals(read) && ((ConsistencyPair)obj).getWrite().equals(write);
    }
  }
}