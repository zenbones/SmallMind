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
package org.smallmind.bayeux.oumuamua.server.spi.json.orthodox;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;
import org.smallmind.bayeux.oumuamua.common.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;

public class OrthodoxArrayValue extends OrthodoxValue implements ArrayValue<OrthodoxValue> {

  private final LinkedList<Value<OrthodoxValue>> valueList = new LinkedList<>();

  protected OrthodoxArrayValue (OrthodoxValueFactory factory) {

    super(factory);
  }

  @Override
  public int size () {

    return valueList.size();
  }

  @Override
  public boolean isEmpty () {

    return valueList.isEmpty();
  }

  @Override
  public Value<OrthodoxValue> get (int index) {

    return valueList.get(index);
  }

  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> add (U value) {

    valueList.add(value);

    return this;
  }

  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> set (int index, U value) {

    valueList.set(index, value);

    return this;
  }

  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> insert (int index, U value) {

    valueList.add(index, value);

    return this;
  }

  @Override
  public Value<OrthodoxValue> remove (int index) {

    return valueList.remove(index);
  }

  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> addAll (Collection<U> values) {

    valueList.addAll(values);

    return this;
  }

  @Override
  public ArrayValue<OrthodoxValue> removeAll () {

    valueList.clear();

    return this;
  }

  @Override
  public void encode (Writer writer)
    throws IOException {

    boolean first = true;

    writer.write('[');
    for (Value<OrthodoxValue> value : valueList) {
      if (value != null) {
        if (!first) {
          writer.write(',');
        }

        value.encode(writer);

        first = false;
      }
    }
    writer.write(']');
  }
}
