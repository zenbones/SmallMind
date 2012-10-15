/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.nosql.hector;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ColumnFamilyUpdater;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.Composite;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.NaturalKey;
import org.smallmind.persistence.cache.CASValue;
import org.smallmind.persistence.cache.WideVectoredDao;
import org.smallmind.persistence.nosql.NoSqlDao;

public abstract class HectorDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends NoSqlDao<I, D> {

  private ColumnFamilyTemplate<Composite, Composite> hectorTemplate;

  public HectorDao (String metricSource, Keyspace keyspace, WideVectoredDao<I, D> wideVectoredDao, boolean cacheEnabled) {

    super(metricSource, wideVectoredDao, cacheEnabled);

    hectorTemplate = new ThriftColumnFamilyTemplate<Composite, Composite>(keyspace, getManagedClass().getSimpleName(), CompositeSerializer.get(), CompositeSerializer.get());
    hectorTemplate.setCount(Integer.MAX_VALUE);
  }

  public abstract Serializer<I> getSerializer ();

  @Override
  public List<D> get (Class<?> parentClass, I id, Class<D> durableClass) {

    return null;
  }

  @Override
  public D[] persist (Class<?> parentClass, I id, Class<D> durableClass, List<D> durables) {

    return null;
  }

  @Override
  public void delete (Class<?> parentClass, I id, Class<D> durableClass, List<D> durables) {

    if ((durables != null) && (!durables.isEmpty())) {

      ColumnFamilyUpdater<Composite, Composite> updater = hectorTemplate.createUpdater(new Composite(parentClass.getSimpleName(), id));
      WideVectoredDao<I, D> wideVectoredDao;

      for (Field nonKeyField : NaturalKey.getNonKeyFields(durableClass)) {

        for (Durable durable : durables) {

          Composite nonKeyComposite = new Composite();

          for (Field naturalKeyField : NaturalKey.getNaturalKeyFields(durableClass)) {
            nonKeyComposite.add(naturalKeyField.get(durable));
          }

          nonKeyComposite.add(nonKeyField.getName());
          updater.deleteColumn(nonKeyComposite);
        }
      }

      if (isCacheEnabled() && ((wideVectoredDao = getWideVectoredDao()) != null)) {

        wideVectoredDao.get(id, )

        CASValue<List<D>> casValue;
        List<D> cachedCopy;
        String key = new StringBuilder("WIDE:").append(durableClass.getSimpleName()).append('=').append(id.toString()).toString();
        boolean removed = false;

        do {
          if ((casValue = getVectoredDao()..getViaCas(key)).getValue() == null){
            break;
          }

          cachedCopy = new LinkedList<AttributeKey>(casValue.getValue());

          for (AttributeKey attributeKey : attributeKeys) {
            if (casValue.getValue().remove(attributeKey)) {
              removed = true;
            }
          }

          if (!removed) {
            break;
          }
        } while (!cache.putViaCas(key, cachedCopy, casValue.getValue(), casValue.getVersion(), 0));
      }
    }

    hectorTemplate.update(updater);
  }
}

