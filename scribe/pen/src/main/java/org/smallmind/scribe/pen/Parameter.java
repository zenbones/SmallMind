package org.smallmind.scribe.pen;

import java.io.Serializable;

public class Parameter implements Serializable {

   private String key;
   private Serializable value;

   public Parameter (String key, Serializable value) {

      this.key = key;
      this.value = value;
   }

   public String getKey () {

      return key;
   }

   public Serializable getValue () {

      return value;
   }
}
