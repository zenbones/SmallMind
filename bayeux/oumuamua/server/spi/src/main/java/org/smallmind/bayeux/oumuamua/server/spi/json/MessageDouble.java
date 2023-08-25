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
import org.smallmind.bayeux.oumuamua.common.api.json.Message;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueFactory;
import org.smallmind.nutsnbolts.util.IterableIterator;

public class MessageDouble<V extends Value<V>> implements Message<V> {

  private final Message<V> innerMessage;
  private final HashSet<String> removedSet = new HashSet<>();
  private Message<V> outerMessage;

  public MessageDouble (Message<V> innerMessage) {

    this.innerMessage = innerMessage;
  }

  @Override
  public Codec<V> getCodec () {

    return innerMessage.getCodec();
  }

  @Override
  public ValueFactory<V> getFactory () {

    return innerMessage.getFactory();
  }

  @Override
  public byte[] encode ()
    throws Exception {

    if (outerMessage == null) {

      return innerMessage.encode();
    } else {

      Message<V> encodingMessage = outerMessage.copy();

      for (String fieldName : new IterableIterator<>(innerMessage.fieldNames())) {
        if (!removedSet.contains(fieldName)) {
          if (outerMessage.get(fieldName) == null) {
            encodingMessage.put(fieldName, innerMessage.get(fieldName));
          }
        }
      }

      return encodingMessage.encode();
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

    for (String fieldName : new IterableIterator<>(innerMessage.fieldNames())) {
      if (!removedSet.contains(fieldName)) {
        nameSet.add(fieldName);
      }
    }

    if (outerMessage != null) {
      for (String fieldName : new IterableIterator<>(outerMessage.fieldNames())) {
        nameSet.add(fieldName);
      }
    }

    return nameSet;
  }

  @Override
  public V get (String field) {

    V value;

    return ((outerMessage == null) || ((value = outerMessage.get(field)) == null)) ? removedSet.contains(field) ? null : innerMessage.get(field) : value;
  }

  @Override
  public V put (String field, Value<V> value) {

    removedSet.remove(field);

    if (outerMessage == null) {
      outerMessage = getCodec().toMessage();
    }

    return outerMessage.put(field, value);
  }

  @Override
  public V remove (String field) {

    V value;

    if ((outerMessage == null) || (value = outerMessage.remove(field)) == null) {
      value = innerMessage.get(field);
    }

    if (value != null) {
      removedSet.add(field);
    }

    return value;
  }

  @Override
  public V removeAll () {

    for (String fieldName : new IterableIterator<>(innerMessage.fieldNames())) {
      removedSet.add(fieldName);
    }

    if (outerMessage != null) {
      outerMessage.removeAll();
    }

    return (V)this;
  }

  @Override
  public Message<V> copy () {

    if (outerMessage == null) {

      return innerMessage.copy();
    } else {

      Message<V> copyingMessage = outerMessage.copy();

      for (String fieldName : new IterableIterator<>(innerMessage.fieldNames())) {
        if (!removedSet.contains(fieldName)) {
          if (outerMessage.get(fieldName) == null) {
            copyingMessage.put(fieldName, innerMessage.get(fieldName));
          }
        }
      }

      return copyingMessage;
    }
  }
}
