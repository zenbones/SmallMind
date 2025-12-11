/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Map-backed object value for the orthodox codec.
 */
public class OrthodoxObjectValue extends OrthodoxValue implements ObjectValue<OrthodoxValue> {

  private final HashMap<String, Value<OrthodoxValue>> valueMap = new HashMap<>();

  /**
   * Creates an empty object value.
   *
   * @param factory owning factory
   */
  protected OrthodoxObjectValue (OrthodoxValueFactory factory) {

    super(factory);
  }

  /**
   * @return number of stored fields
   */
  @Override
  public int size () {

    return valueMap.size();
  }

  /**
   * @return {@code true} if no fields exist
   */
  @Override
  public boolean isEmpty () {

    return valueMap.isEmpty();
  }

  /**
   * @return iterator over field names
   */
  @Override
  public Iterator<String> fieldNames () {

    return valueMap.keySet().iterator();
  }

  /**
   * Retrieves a field value.
   *
   * @param field field name
   * @return stored value or {@code null}
   */
  @Override
  public Value<OrthodoxValue> get (String field) {

    return valueMap.get(field);
  }

  /**
   * Adds or replaces a field.
   *
   * @param field field name
   * @param value value to store
   * @return this object
   */
  @Override
  public <U extends Value<OrthodoxValue>> ObjectValue<OrthodoxValue> put (String field, U value) {

    valueMap.put(field, value);

    return this;
  }

  /**
   * Removes a field by name.
   *
   * @param field field to remove
   * @return removed value or {@code null}
   */
  @Override
  public Value<OrthodoxValue> remove (String field) {

    return valueMap.remove(field);
  }

  /**
   * Clears all fields.
   *
   * @return this object
   */
  @Override
  public ObjectValue<OrthodoxValue> removeAll () {

    valueMap.clear();

    return this;
  }

  /**
   * Encodes the object to JSON.
   *
   * @param writer destination writer
   * @throws IOException if writing fails
   */
  @Override
  public void encode (Writer writer)
    throws IOException {

    boolean first = true;

    writer.write('{');

    for (Map.Entry<String, Value<OrthodoxValue>> valueEntry : valueMap.entrySet()) {
      if (valueEntry.getValue() != null) {
        if (!first) {
          writer.write(',');
        }

        writer.write('"');
        writer.write(valueEntry.getKey());
        writer.write("\":");
        valueEntry.getValue().encode(writer);

        first = false;
      }
    }

    writer.write('}');
  }
}
