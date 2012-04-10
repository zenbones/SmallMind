/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.pen.probe;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Metric implements Serializable {

   private ConcurrentHashMap<String, Serializable> dataMap;
   private String title;

   public Metric () {

      dataMap = new ConcurrentHashMap<String, Serializable>();
   }

   public Metric (String title) {

      this();

      this.title = title;
   }

   public Metric (Metric metric) {

      this(metric.getTitle());

      synchronized (metric) {
         dataMap.putAll(metric.getDataMap());
      }
   }

   private Map<String, Serializable> getDataMap () {

      return dataMap;
   }

   public String getTitle () {

      return (title == null) ? getClass().getSimpleName() : title;
   }

   public Set<String> getKeys () {

      return dataMap.keySet();
   }

   public boolean containsKey (String key) {

      return dataMap.containsKey(key);
   }

   public Object getData (String key) {

      return dataMap.get(key);
   }

   protected void setData (String key, Number numberValue) {

      dataMap.put(key, numberValue);
   }

   protected void setData (String key, Boolean booleanValue) {

      dataMap.put(key, booleanValue);
   }

   protected void setData (String key, Character characterValue) {

      dataMap.put(key, characterValue);
   }

   protected void setData (String key, String stringValue) {

      dataMap.put(key, stringValue);
   }
}
