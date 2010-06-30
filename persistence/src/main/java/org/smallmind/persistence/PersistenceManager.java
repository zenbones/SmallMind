package org.smallmind.persistence;

public class PersistenceManager {

   private static Persistence PERSISTENCE;

   public static void register (Persistence persistence) {

      PERSISTENCE = persistence;
   }

   public static Persistence getPersistence () {

      return PERSISTENCE;
   }
}
