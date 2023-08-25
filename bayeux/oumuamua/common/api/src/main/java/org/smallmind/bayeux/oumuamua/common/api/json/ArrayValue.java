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
package org.smallmind.bayeux.oumuamua.common.api.json;

import java.util.Collection;

public interface ArrayValue<V extends Value<V>> extends Value<V>, Iterable<V> {

  default ValueType getType () {

    return ValueType.ARRAY;
  }

  default ArrayValue<V> add (boolean bool) {

    return add(getFactory().booleanValue(bool));
  }

  default ArrayValue<V> set (int index, boolean bool) {

    return set(index, getFactory().booleanValue(bool));
  }

  default ArrayValue<V> insert (int index, boolean bool) {

    return insert(index, getFactory().booleanValue(bool));
  }

  default ArrayValue<V> add (int i) {

    return add(getFactory().numberValue(i));
  }

  default ArrayValue<V> set (int index, int i) {

    return set(index, getFactory().numberValue(i));
  }

  default ArrayValue<V> insert (int index, int i) {

    return insert(index, getFactory().numberValue(i));
  }

  default ArrayValue<V> add (long l) {

    return add(getFactory().numberValue(l));
  }

  default ArrayValue<V> set (int index, long l) {

    return set(index, getFactory().numberValue(l));
  }

  default ArrayValue<V> insert (int index, long l) {

    return insert(index, getFactory().numberValue(l));
  }

  default ArrayValue<V> add (double d) {

    return add(getFactory().numberValue(d));
  }

  default ArrayValue<V> set (int index, double d) {

    return set(index, getFactory().numberValue(d));
  }

  default ArrayValue<V> insert (int index, double d) {

    return insert(index, getFactory().numberValue(d));
  }

  default ArrayValue<V> add (String text) {

    return add(getFactory().textValue(text));
  }

  default ArrayValue<V> set (int index, String text) {

    return set(index, getFactory().textValue(text));
  }

  default ArrayValue<V> insert (int index, String text) {

    return insert(index, getFactory().textValue(text));
  }

  int size ();

  boolean isEmpty ();

  V get (int index);

  <U extends Value<V>> ArrayValue<V> add (U value);

  <U extends Value<V>> ArrayValue<V> set (int index, U value);

  <U extends Value<V>> ArrayValue<V> insert (int index, U value);

  V remove (int index);

  <U extends Value<V>> ArrayValue<V> addAll (Collection<U> values);

  ArrayValue<V> removeAll ();
}
