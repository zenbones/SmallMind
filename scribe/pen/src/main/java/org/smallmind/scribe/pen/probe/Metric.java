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
