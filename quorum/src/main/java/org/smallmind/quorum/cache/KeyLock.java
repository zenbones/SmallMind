package org.smallmind.quorum.cache;

import org.smallmind.nutsnbolts.util.UniqueId;
import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public class KeyLock {

   private UniqueId uniqueId;

   public KeyLock () {

      uniqueId = UniqueId.newInstance();
   }

   public String getName () {

      return "RotaryLock-" + uniqueId.generateBigInteger();
   }

   public UniqueId getUniqueId () {

      return uniqueId;
   }

   public int hashCode () {

      return uniqueId.hashCode();
   }

   public boolean equals (Object obj) {

      return (obj instanceof KeyLock) && uniqueId.equals(((KeyLock)obj).getUniqueId());
   }
}
