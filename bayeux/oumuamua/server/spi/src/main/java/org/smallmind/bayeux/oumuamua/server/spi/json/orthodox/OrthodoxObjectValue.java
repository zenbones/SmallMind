/*
 * Copyright (c) 2007 through 2026 David Berkman
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
 * {@link HashMap}-backed {@link ObjectValue} implementation for the orthodox codec, providing
 * unordered field storage with constant-time get, put, and remove operations.
 */
public class OrthodoxObjectValue extends OrthodoxValue implements ObjectValue<OrthodoxValue> {

  private final HashMap<String, Value<OrthodoxValue>> valueMap = new HashMap<>();

  /**
   * Constructs an empty object value associated with the given factory.
   *
   * @param factory the {@link OrthodoxValueFactory} that owns this value
   */
  protected OrthodoxObjectValue (OrthodoxValueFactory factory) {

    super(factory);
  }

  /**
   * Returns the number of fields currently stored in the object.
   *
   * @return field count
   */
  @Override
  public int size () {

    return valueMap.size();
  }

  /**
   * Reports whether the object contains no fields.
   *
   * @return {@code true} when no fields have been stored
   */
  @Override
  public boolean isEmpty () {

    return valueMap.isEmpty();
  }

  /**
   * Returns an iterator over the names of all stored fields in unspecified order.
   *
   * @return field name iterator reflecting the current map key set
   */
  @Override
  public Iterator<String> fieldNames () {

    return valueMap.keySet().iterator();
  }

  /**
   * Returns the value stored under {@code field}, or {@code null} if the field is absent.
   *
   * @param field name of the field to look up
   * @return the associated value, or {@code null} if no such field exists
   */
  @Override
  public Value<OrthodoxValue> get (String field) {

    return valueMap.get(field);
  }

  /**
   * Stores {@code value} under {@code field}, replacing any previously stored value.
   *
   * @param field name of the field to set
   * @param value value to associate with the field
   * @return this object for chaining
   */
  @Override
  public <U extends Value<OrthodoxValue>> ObjectValue<OrthodoxValue> put (String field, U value) {

    valueMap.put(field, value);

    return this;
  }

  /**
   * Removes the field named {@code field} and returns its former value.
   *
   * @param field name of the field to remove
   * @return the value previously associated with the field, or {@code null} if absent
   */
  @Override
  public Value<OrthodoxValue> remove (String field) {

    return valueMap.remove(field);
  }

  /**
   * Removes all fields from the object, leaving it empty.
   *
   * @return this object for chaining
   */
  @Override
  public ObjectValue<OrthodoxValue> removeAll () {

    valueMap.clear();

    return this;
  }

  /**
   * Writes the JSON object representation of all non-null fields to {@code writer}.
   *
   * @param writer destination for the JSON output
   * @throws IOException if writing to {@code writer} fails
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
