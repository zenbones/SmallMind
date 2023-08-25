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
package org.smallmind.bayeux.oumuamua.server.spi.json;

import java.util.HashSet;
import java.util.Iterator;
import org.smallmind.bayeux.oumuamua.common.api.Codec;
import org.smallmind.bayeux.oumuamua.common.api.json.Body;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueFactory;
import org.smallmind.nutsnbolts.util.IterableIterator;

public class BodyDouble<V extends Value<V>> implements Body<V> {

  private final Body<V> innerBody;
  private final HashSet<String> removedSet = new HashSet<>();
  private Body<V> outerBody;

  public BodyDouble (Body<V> innerBody) {

    this.innerBody = innerBody;
  }

  @Override
  public Codec<V> getCodec () {

    return innerBody.getCodec();
  }

  @Override
  public ValueFactory<V> getFactory () {

    return innerBody.getFactory();
  }

  @Override
  public byte[] encode ()
    throws Exception {

    if (outerBody == null) {

      return innerBody.encode();
    } else {

      Body<V> encodingBody = outerBody.copy();

      for (String fieldName : new IterableIterator<>(innerBody.fieldNames())) {
        if (!removedSet.contains(fieldName)) {
          if (outerBody.get(fieldName) == null) {
            encodingBody.put(fieldName, innerBody.get(fieldName));
          }
        }
      }

      return encodingBody.encode();
    }
  }

  @Override
  public int size () {

    return fieldNameSet().size();
  }

  @Override
  public boolean isEmpty () {

    return size() == 0;
  }

  @Override
  public Iterator<String> fieldNames () {

    return fieldNameSet().iterator();
  }

  private HashSet<String> fieldNameSet () {

    HashSet<String> nameSet = new HashSet<>();

    for (String fieldName : new IterableIterator<>(innerBody.fieldNames())) {
      if (!removedSet.contains(fieldName)) {
        nameSet.add(fieldName);
      }
    }

    if (outerBody != null) {
      for (String fieldName : new IterableIterator<>(outerBody.fieldNames())) {
        nameSet.add(fieldName);
      }
    }

    return nameSet;
  }

  @Override
  public V get (String field) {

    V value;

    return ((outerBody == null) || ((value = outerBody.get(field)) == null)) ? removedSet.contains(field) ? null : innerBody.get(field) : value;
  }

  @Override
  public V put (String field, Value<V> value) {

    removedSet.remove(field);

    if (outerBody == null) {
      outerBody = getCodec().toBody();
    }

    return outerBody.put(field, value);
  }

  @Override
  public V remove (String field) {

    V value;

    if ((outerBody == null) || (value = outerBody.remove(field)) == null) {
      value = innerBody.get(field);
    }

    if (value != null) {
      removedSet.add(field);
    }

    return value;
  }

  @Override
  public V removeAll () {

    for (String fieldName : new IterableIterator<>(innerBody.fieldNames())) {
      removedSet.add(fieldName);
    }

    if (outerBody != null) {
      outerBody.removeAll();
    }

    return (V)this;
  }

  @Override
  public Body<V> copy () {

    if (outerBody == null) {

      return innerBody.copy();
    } else {

      Body<V> copyingBody = outerBody.copy();

      for (String fieldName : new IterableIterator<>(innerBody.fieldNames())) {
        if (!removedSet.contains(fieldName)) {
          if (outerBody.get(fieldName) == null) {
            copyingBody.put(fieldName, innerBody.get(fieldName));
          }
        }
      }

      return copyingBody;
    }
  }
}
