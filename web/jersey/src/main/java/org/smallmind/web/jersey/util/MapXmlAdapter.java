/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.web.jersey.util;

import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public abstract class MapXmlAdapter<M extends Map<K, V>, K, V> extends XmlAdapter<MapKeyValue<K, V>[], M> {

  public abstract M getEmptyMap ();

  public abstract Class<K> getKeyClass ();

  public abstract Class<V> getValueClass ();

  @Override
  public M unmarshal (MapKeyValue<K, V>[] array) throws Exception {

    if (array != null) {

      M map = getEmptyMap();

      for (MapKeyValue<K, V> mapKeyValue : array) {
        map.put(JsonCodec.convert(mapKeyValue.getKey(), getKeyClass()), JsonCodec.convert(mapKeyValue.getValue(), getValueClass()));
      }

      return map;
    }

    return null;
  }

  @Override
  public MapKeyValue<K, V>[] marshal (M map) throws Exception {

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
