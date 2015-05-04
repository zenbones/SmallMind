/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.nosql.spring.hector;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import me.prettyprint.cassandra.model.BasicKeyspaceDefinition;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.NaturalKey;
import org.smallmind.persistence.nosql.hector.HectorType;

public class HectorSchemaVerifier {

  public static void verify (Cluster cluster, Keyspace keyspace, String replicationStrategyClass, int replicationFactor) {

    KeyspaceDefinition keyspaceDefinition;
    List<Class> hectorTypes = new LinkedList<Class>(HectorFileSeekingBeanFactoryPostProcessor.getHectorTypes(keyspace.getKeyspaceName()));

    while ((keyspaceDefinition = cluster.describeKeyspace(keyspace.getKeyspaceName())) == null) {

      BasicKeyspaceDefinition avatarKeyspace = new BasicKeyspaceDefinition();
      avatarKeyspace.setName(keyspace.getKeyspaceName());
      avatarKeyspace.setDurableWrites(true);
      avatarKeyspace.setStrategyClass(replicationStrategyClass);
      avatarKeyspace.setReplicationFactor(replicationFactor);

      cluster.addKeyspace(avatarKeyspace, true);
    }

    for (ColumnFamilyDefinition columnFamilyDefinition : keyspaceDefinition.getCfDefs()) {

      Iterator<Class> hectorTypeIter = hectorTypes.iterator();

      while (hectorTypeIter.hasNext()) {
        if (hectorTypeIter.next().getSimpleName().equals(columnFamilyDefinition.getName())) {
          hectorTypeIter.remove();
          break;
        }
      }
    }

    for (Class hectorType : hectorTypes) {

      ColumnFamilyDefinition attributeKeyFamilyDefinition = HFactory.createColumnFamilyDefinition(keyspace.getKeyspaceName(), hectorType.getSimpleName(), ComparatorType.COMPOSITETYPE);
      attributeKeyFamilyDefinition.setColumnType(ColumnType.STANDARD);
      attributeKeyFamilyDefinition.setKeyValidationClass(ComparatorType.COMPOSITETYPE.getClassName() + "(UTF8Type, UTF8Type, LongType)");
      attributeKeyFamilyDefinition.setDefaultValidationClass(ComparatorType.BYTESTYPE.getClassName());

      attributeKeyFamilyDefinition.setComparatorTypeAlias(composeTypeAlias(hectorType));

      cluster.addColumnFamily(attributeKeyFamilyDefinition, true);
    }
  }

  private static String composeTypeAlias (Class<? extends Durable> durableType) {

    StringBuilder aliasBuilder = new StringBuilder("(");

    for (Field naturalKeyField : NaturalKey.getNaturalKeyFields(durableType)) {
      aliasBuilder.append(HectorType.getTranslator(naturalKeyField.getType(), naturalKeyField.getName()).getHectorType()).append(',');
    }

    return aliasBuilder.append("UTF8Type)").toString();
  }
}
