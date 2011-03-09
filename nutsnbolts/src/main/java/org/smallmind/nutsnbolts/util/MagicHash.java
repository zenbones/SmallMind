package org.smallmind.nutsnbolts.util;

public class MagicHash {

   public static int rehash (int hash) {

      hash ^= (hash >>> 20) ^ (hash >>> 12);

      return hash ^ (hash >>> 7) ^ (hash >>> 4);
   }
}
