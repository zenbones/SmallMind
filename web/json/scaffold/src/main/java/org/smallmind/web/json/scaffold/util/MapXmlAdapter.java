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
package org.smallmind.web.json.scaffold.util;

import java.util.Map;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Base JAXB adapter for serializing/deserializing {@link Map} instances as arrays of {@link MapKeyValue}.
 *
 * @param <M> concrete map type
 * @param <K> key type
 * @param <V> value type
 */
public abstract class MapXmlAdapter<M extends Map<K, V>, K, V> extends XmlAdapter<MapKeyValue<K, V>[], M> {

  /**
   * @return an empty mutable map of the desired type
   */
  public abstract M getEmptyMap ();

  /**
   * @return class used for converting keys
   */
  public abstract Class<K> getKeyClass ();

  /**
   * @return class used for converting values
   */
  public abstract Class<V> getValueClass ();

  /**
   * Converts an array of key/value pairs into a map instance.
   *
   * @param array serialized map entries
   * @return populated map or {@code null} if the array is {@code null}
   */
  @Override
  public M unmarshal (MapKeyValue<K, V>[] array) {

    if (array != null) {

      M map = getEmptyMap();

      for (MapKeyValue<K, V> mapKeyValue : array) {
        map.put(JsonCodec.convert(mapKeyValue.getKey(), getKeyClass()), JsonCodec.convert(mapKeyValue.getValue(), getValueClass()));
      }

      return map;
    }

    return null;
  }

  /**
   * Serializes a map into an array of key/value pairs.
   *
   * @param map map to serialize
   * @return serialized array or {@code null} if the map is {@code null}
   */
  @Override
  public MapKeyValue<K, V>[] marshal (M map) {

    if (map != null) {

      MapKeyValue<K, V>[] array = new MapKeyValue[map.size()];
      int index = 0;

      for (Map.Entry<K, V> entry : map.entrySet()) {
        array[index++] = new MapKeyValue<>(entry.getKey(), entry.getValue());
      }

      return array;
    }

    return null;
  }
}
