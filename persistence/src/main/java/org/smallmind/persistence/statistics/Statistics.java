package org.smallmind.persistence.statistics;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.smallmind.persistence.Durable;

public class Statistics implements Serializable {

   // Map<Durable Name, Map<Method Name, Map<Stat Source, StatLine>>>
   private HashMap<String, HashMap<String, HashMap<String, StatLine>>> lineMap = new HashMap<String, HashMap<String, HashMap<String, StatLine>>>();

   public synchronized HashMap<String, HashMap<String, HashMap<String, StatLine>>> getLineMap () {

      return lineMap;
   }

   public synchronized void addStatLine (Class<? extends Durable> durableClass, Method method, String source, long time) {

      HashMap<String, HashMap<String, StatLine>> methodMap;
      HashMap<String, StatLine> sourceMap;
      StatLine statLine;

      if ((methodMap = lineMap.get(durableClass.getSimpleName())) == null) {
         lineMap.put(durableClass.getSimpleName(), methodMap = new HashMap<String, HashMap<String, StatLine>>());
      }

      if ((sourceMap = methodMap.get(method.getName())) == null) {
         methodMap.put(method.getName(), sourceMap = new HashMap<String, StatLine>());
      }

      if ((statLine = sourceMap.get(source)) == null) {
         sourceMap.put(source, statLine = new StatLine(source));
      }

      statLine.hit(time);
   }
}
