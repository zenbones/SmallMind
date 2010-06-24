package org.smallmind.nutsnbolts.resource;

import java.util.Arrays;

public class ResourceSchemes {

   private String[] schemes;

   public ResourceSchemes (String[] schemes) {

      this.schemes = schemes;
   }

   public boolean containsScheme (String scheme) {

      for (String matchingScheme : schemes) {
         if (matchingScheme.equals(scheme)) {
            return true;
         }
      }

      return false;
   }

   public int hashCode () {

      return Arrays.hashCode(schemes);
   }

   public boolean equals (Object obj) {

      return (obj instanceof String[]) && Arrays.equals(schemes, (String[])obj);
   }
}
