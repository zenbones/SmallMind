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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.template.ColumnFamilyResult;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ColumnFamilyUpdater;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.Composite;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.DurableFields;
import org.smallmind.persistence.NaturalKey;
import org.smallmind.persistence.PersistenceException;
import org.smallmind.persistence.cache.WideVectoredDao;
import org.smallmind.persistence.nosql.NoSqlDao;

public abstract class HectorDao<W extends Serializable & Comparable<W>, I extends Serializable & Comparable<I>, D extends Durable<I>> extends NoSqlDao<W, I, D> {

  private ColumnFamilyTemplate<Composite, Composite> hectorTemplate;

  public HectorDao (String metricSource, Keyspace keyspace, WideVectoredDao<W, I, D> wideVectoredDao, boolean cacheEnabled) {

    super(metricSource, wideVectoredDao, cacheEnabled);

    hectorTemplate = new ThriftColumnFamilyTemplate<Composite, Composite>(keyspace, getManagedClass().getSimpleName(), CompositeSerializer.get(), CompositeSerializer.get());
    hectorTemplate.setCount(Integer.MAX_VALUE);
  }

  public abstract I createId ();

  public abstract HectorTranslator getIdTranslator ();

  public abstract HectorTranslator getParentIdTranslator ();

  @Override
  public List<D> get (Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass) {

    List<D> durables;
    WideVectoredDao<W, I, D> wideVectoredDao;
    ColumnFamilyResult<Composite, Composite> hectorResult;

    if (isCacheEnabled() && ((wideVectoredDao = getWideVectoredDao()) != null)) {
      if ((durables = wideVectoredDao.get(parentClass, parentId, durableClass)) != null) {

        return durables;
      }
    }

    if ((hectorResult = hectorTemplate.queryColumns(new Composite(parentClass.getSimpleName(), getParentIdTranslator().toHectorValue(parentId)))).hasResults()) {

      HashMap<NaturalKey, D> naturalMap = new HashMap<NaturalKey, D>();

      for (Composite columnName : hectorResult.getColumnNames()) {

        D durable;
        NaturalKey naturalKey;
        Field[] naturalKeyFields = NaturalKey.getNaturalKeyFields(durableClass);
        Field nonKeyField;
        Object[] naturalKeyValues = new Object[naturalKeyFields.length];
        String nonKeyFieldName;
        int naturalKeyIndex;

        if ((nonKeyField = DurableFields.getField(durableClass, nonKeyFieldName = columnName.get(naturalKeyFields.length, StringSerializer.get()))) == null) {
          throw new PersistenceException("Unknown field(%s) in return values", nonKeyFieldName);
        }

        naturalKeyIndex = 0;
        for (Field naturalKeyField : naturalKeyFields) {
          naturalKeyValues[naturalKeyIndex] = HectorType.getTranslator(naturalKeyField.getType(), naturalKeyField.getName()).toEntityValue(naturalKeyField.getType(), naturalKeyIndex++, columnName);
        }

        if ((durable = naturalMap.get(naturalKey = new NaturalKey<D>(durableClass, naturalKeyValues))) == null) {
          try {
            naturalMap.put(naturalKey, durable = durableClass.newInstance());

            naturalKeyIndex = 0;
            for (Field naturalKeyField : naturalKeyFields) {
              naturalKeyField.set(durable, naturalKeyValues[naturalKeyIndex++]);
            }
          }
          catch (Exception exception) {
            throw new PersistenceException(exception);
          }
        }

        try {
          if (nonKeyFieldName.equals("id")) {
            nonKeyField.set(durable, getIdTranslator().toEntityValue(getIdClass(), columnName, hectorResult));
          }
          else {

            Class<?> nonKeyType = nonKeyField.getType();

            if (Date.class.isAssignableFrom(nonKeyType)) {
              nonKeyField.set(durable, hectorResult.getDate(columnName));
            }
            else if (nonKeyType.isEnum()) {

              String enumName = hectorResult.getString(columnName);
              boolean matched = false;

              for (Object enumConstant : nonKeyType.getEnumConstants()) {
                if (((Enum)enumConstant).name().equals(enumName)) {
                  nonKeyField.set(durable, enumConstant);
                  matched = true;
                  break;
                }
              }

              if (!matched) {
                throw new PersistenceException("Unable to locate matching enum constant(%s) for field(%s) of type(%s)", enumName, nonKeyFieldName, nonKeyType.getName());
              }
            }
            else if (CharSequence.class.isAssignableFrom(nonKeyType)) {
              nonKeyField.set(durable, hectorResult.getString(columnName));
            }
            else if (long.class.equals(nonKeyType) || (Long.class.equals(nonKeyType))) {
              nonKeyField.setLong(durable, hectorResult.getLong(columnName));
            }
            else if (boolean.class.equals(nonKeyType) || (Boolean.class.equals(nonKeyType))) {
              nonKeyField.setBoolean(durable, hectorResult.getBoolean(columnName));
            }
            else if (int.class.equals(nonKeyType) || (Integer.class.equals(nonKeyType))) {
              nonKeyField.setInt(durable, hectorResult.getLong(columnName).intValue());
            }
            else if (double.class.equals(nonKeyType) || (Double.class.equals(nonKeyType))) {
              nonKeyField.setDouble(durable, hectorResult.getDouble(columnName));
            }
            else if (float.class.equals(nonKeyType) || (Float.class.equals(nonKeyType))) {
              nonKeyField.setFloat(durable, hectorResult.getDouble(columnName).floatValue());
            }
            else if (char.class.equals(nonKeyType) || (Character.class.equals(nonKeyType))) {
              nonKeyField.setChar(durable, hectorResult.getString(columnName).charAt(0));
            }
            else if (short.class.equals(nonKeyType) || (Short.class.equals(nonKeyType))) {
              nonKeyField.setShort(durable, hectorResult.getLong(columnName).shortValue());
            }
            else if (byte.class.equals(nonKeyType) || (Byte.class.equals(nonKeyType))) {
              nonKeyField.setByte(durable, hectorResult.getLong(columnName).byteValue());
            }
            else {
              throw new PersistenceException("Unknown field(%s) type(%s)", nonKeyField.getName(), nonKeyType.getName());
            }
          }
        }
        catch (IllegalAccessException illegalAccessException) {
          throw new PersistenceException(illegalAccessException);
        }
      }

      durables = new LinkedList<D>(naturalMap.values());

      if (isCacheEnabled() && ((wideVectoredDao = getWideVectoredDao()) != null)) {
        wideVectoredDao.persist(parentClass, parentId, durableClass, durables);
      }

      return durables;
    }

    return Collections.emptyList();
  }

  @Override
  public List<D> persist (Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, List<D> durables) {

    if ((durables != null) && (!durables.isEmpty())) {

      ColumnFamilyUpdater<Composite, Composite> updater = hectorTemplate.createUpdater(new Composite(parentClass.getSimpleName(), getParentIdTranslator().toHectorValue(parentId)));
      WideVectoredDao<W, I, D> wideVectoredDao;

      try {
        for (Field nonKeyField : NaturalKey.getNonKeyFields(durableClass)) {

          for (D durable : durables) {

            I id;

            if ((id = durable.getId()) == null) {
              durable.setId(id = createId());
            }

            Composite nonKeyComposite = new Composite();

            for (Field naturalKeyField : NaturalKey.getNaturalKeyFields(durableClass)) {
              nonKeyComposite.add(HectorType.getTranslator(naturalKeyField.getType(), naturalKeyField.getName()).toHectorValue(naturalKeyField.getType().cast(naturalKeyField.get(durable))));
            }

            nonKeyComposite.add(nonKeyField.getName());

            if (nonKeyField.getName().equals("id")) {
              updater.setValue(nonKeyComposite, getIdTranslator().toHectorValue(id), getIdTranslator().getSerializer());
            }
            else {

              Class<?> nonKeyType = nonKeyField.getType();

              if (nonKeyField.get(durable) == null) {
                updater.deleteColumn(nonKeyComposite);
              }
              else if (Date.class.isAssignableFrom(nonKeyType)) {
                updater.setDate(nonKeyComposite, (Date)nonKeyField.get(durable));
              }
              else if (nonKeyType.isEnum()) {
                updater.setString(nonKeyComposite, ((Enum)nonKeyField.get(durable)).name());
              }
              else if (CharSequence.class.isAssignableFrom(nonKeyType)) {
                updater.setString(nonKeyComposite, nonKeyField.get(durable).toString());
              }
              else if (long.class.equals(nonKeyType) || (Long.class.equals(nonKeyType))) {
                updater.setLong(nonKeyComposite, nonKeyField.getLong(durable));
              }
              else if (boolean.class.equals(nonKeyType) || (Boolean.class.equals(nonKeyType))) {
                updater.setBoolean(nonKeyComposite, nonKeyField.getBoolean(durable));
              }
              else if (int.class.equals(nonKeyType) || (Integer.class.equals(nonKeyType))) {
                updater.setLong(nonKeyComposite, (long)nonKeyField.getInt(durable));
              }
              else if (double.class.equals(nonKeyType) || (Double.class.equals(nonKeyType))) {
                updater.setDouble(nonKeyComposite, nonKeyField.getDouble(durable));
              }
              else if (float.class.equals(nonKeyType) || (Float.class.equals(nonKeyType))) {
                updater.setDouble(nonKeyComposite, (double)nonKeyField.getFloat(durable));
              }
              else if (char.class.equals(nonKeyType) || (Character.class.equals(nonKeyType))) {
                updater.setString(nonKeyComposite, new String(new char[] {nonKeyField.getChar(durable)}));
              }
              else if (short.class.equals(nonKeyType) || (Short.class.equals(nonKeyType))) {
                updater.setLong(nonKeyComposite, (long)nonKeyField.getShort(durable));
              }
              else if (byte.class.equals(nonKeyType) || (Byte.class.equals(nonKeyType))) {
                updater.setLong(nonKeyComposite, (long)nonKeyField.getByte(durable));
              }
              else {
                throw new PersistenceException("Unknown field(%s) type(%s)", nonKeyField.getName(), nonKeyType.getName());
              }
            }
          }
        }
      }
      catch (IllegalAccessException illegalAccessException) {
        throw new PersistenceException(illegalAccessException);
      }

      if (isCacheEnabled() && ((wideVectoredDao = getWideVectoredDao()) != null)) {

        List<D> cachedInstance;

        if ((cachedInstance = wideVectoredDao.get(parentClass, parentId, durableClass)) != null) {
          for (D durable : durables) {

            int cachedIndex;

            if ((cachedIndex = cachedInstance.indexOf(durable)) >= 0) {
              cachedInstance.set(cachedIndex, durable);
            }
            else {
              cachedInstance.add(durable);
            }
          }

          wideVectoredDao.persist(parentClass, parentId, durableClass, durables);
        }
      }

      hectorTemplate.update(updater);
    }

    return durables;
  }

  @Override
  public void delete (Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, List<D> durables) {

    if ((durables != null) && (!durables.isEmpty())) {

      ColumnFamilyUpdater<Composite, Composite> updater = hectorTemplate.createUpdater(new Composite(parentClass.getSimpleName(), getParentIdTranslator().toHectorValue(parentId)));
      WideVectoredDao<W, I, D> wideVectoredDao;

      try {
        for (Field nonKeyField : NaturalKey.getNonKeyFields(durableClass)) {

          for (D durable : durables) {

            Composite nonKeyComposite = new Composite();

            for (Field naturalKeyField : NaturalKey.getNaturalKeyFields(durableClass)) {
              nonKeyComposite.add(HectorType.getTranslator(naturalKeyField.getType(), naturalKeyField.getName()).toHectorValue(naturalKeyField.get(durable)));
            }

            nonKeyComposite.add(nonKeyField.getName());
            updater.deleteColumn(nonKeyComposite);
          }
        }
      }
      catch (IllegalAccessException illegalAccessException) {
        throw new PersistenceException(illegalAccessException);
      }

      if (isCacheEnabled() && ((wideVectoredDao = getWideVectoredDao()) != null)) {

        List<D> cachedInstance;

        if ((cachedInstance = wideVectoredDao.get(parentClass, parentId, durableClass)) != null) {
          for (D durable : durables) {
            cachedInstance.remove(durable);
          }

          wideVectoredDao.persist(parentClass, parentId, durableClass, durables);
        }
      }

      hectorTemplate.update(updater);
    }
  }
}

