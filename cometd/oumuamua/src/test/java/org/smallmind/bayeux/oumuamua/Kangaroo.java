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
package org.smallmind.bayeux.oumuamua;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.smallmind.bayeux.cometd.message.ExtMapLike;
import org.smallmind.bayeux.cometd.message.ListLike;
import org.smallmind.bayeux.cometd.message.MapLike;

public class Kangaroo {

  public static void main (String... args)
    throws Exception {

    MapLike m = new MapLike( JsonNodeFactory.instance.objectNode());

    m.put("first", new HashMap<String, Object>());
    System.out.println(m.encode());
    System.out.println(m.encode());
    m.put("second", Collections.emptyList());
    System.out.println(m.encode());
    System.out.println(m.encode());
    ((List)m.get("second")).add(1);
    System.out.println(m.encode());
    System.out.println(m.encode());
    ((List)m.get("second")).addAll(List.of(2, 3, 4, 5, 6, 7, 8, 9, 0));
    System.out.println(m.encode());
    System.out.println(m.encode());
    List subList = ((List)m.get("second")).subList(2, 6);
    System.out.println(((ListLike)subList).encode());
    System.out.println(((ListLike)subList).encode());
    subList.add(2, "A");
    subList.remove(3);
    System.out.println(((ListLike)subList).encode());
    System.out.println(((ListLike)subList).encode());
    System.out.println(m.encode());
    System.out.println(m.encode());
    m.put("ext", Map.of("ack", Map.of("one", 8)));
    System.out.println(m.encode());
    System.out.println(m.encode());

    ExtMapLike e = new ExtMapLike(m);
    System.out.println("------------------------------------");

    System.out.println(e.encode());
    System.out.println(e.encode());

    ((Map)((Map)e.get("ext")).get("ack")).put("one", 10);
    System.out.println(e.encode());
    System.out.println(e.encode());

    ((Map)e.get("ext")).put("auth", "foobar");
    ((Map)e.get("ext")).put("custom", Map.of("a", "b"));
    System.out.println(e.encode());
    System.out.println(e.encode());
    ((Map)((Map)e.get("ext")).get("custom")).put("c", "d");
    System.out.println(e.encode());
    System.out.println(e.encode());
    e.remove("ext");
    System.out.println(e.encode());
    System.out.println(e.encode());
    ((Map)e.createIfAbsentMapLike("ext")).put("ack", 8);
    System.out.println(e.encode());
    System.out.println(e.encode());
    ((Map)e.createIfAbsentMapLike("ext")).put("auth", "foobar");
    System.out.println(e.encode());
    System.out.println(e.encode());

    System.out.println(m.encode());
  }
}
