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
package org.smallmind.phalanx.wire;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.smallmind.web.jersey.util.JsonCodec;

public class WireContextXmlAdapter extends XmlAdapter<Object[], WireContext[]> {

  @Override
  public WireContext[] unmarshal (Object[] objects) {

    WireContext[] contexts;
    LinkedList<WireContext> contextList = new LinkedList<>();

    if (objects != null) {
      for (Object obj : objects) {

        LinkedHashMap<String, Object> objectMap;

        if ((objectMap = (LinkedHashMap<String, Object>)obj).size() == 1) {

          Map.Entry<String, Object> topEntry = objectMap.entrySet().iterator().next();
          Class<? extends WireContext> contextClass;

          if ((contextClass = WireContextManager.getContextClass(topEntry.getKey())) != null) {
            contextList.add(JsonCodec.convert(topEntry.getValue(), contextClass));
          } else {
            contextList.add(new ProtoWireContext(topEntry.getKey(), topEntry.getValue()));
          }
        }
      }
    }

    contexts = new WireContext[contextList.size()];
    contextList.toArray(contexts);

    return contexts;
  }

  @Override
  public Object[] marshal (WireContext[] wireContexts) {

    if (wireContexts == null) {

      return null;
    }

    Object[] objects = new Object[wireContexts.length];
    int index = 0;

    for (WireContext wireContext : wireContexts) {
      if (wireContext instanceof ProtoWireContext) {

        LinkedHashMap<String, Object> objectMap = new LinkedHashMap<>();

        objectMap.put(((ProtoWireContext)wireContext).getSkin(), ((ProtoWireContext)wireContext).getGuts());
        objects[index++] = objectMap;
      } else {

        LinkedHashMap<String, Object> objectMap = new LinkedHashMap<>();
        XmlRootElement xmlRootElementAnnotation = wireContext.getClass().getAnnotation(XmlRootElement.class);

        objectMap.put((xmlRootElementAnnotation == null) ? wireContext.getClass().getSimpleName() : xmlRootElementAnnotation.name(), wireContext);
        objects[index++] = objectMap;
      }
    }

    return objects;
  }
}