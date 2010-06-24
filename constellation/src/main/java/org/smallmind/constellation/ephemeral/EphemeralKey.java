package org.smallmind.constellation.ephemeral;

import java.io.Serializable;

public class EphemeralKey implements Serializable {

   private String hostAddress;
   private String ephemeralId;

   public EphemeralKey (String hostAddress, String ephemeralId) {

      this.hostAddress = hostAddress;
      this.ephemeralId = ephemeralId;
   }

   public String getHostAddress () {

      return hostAddress;
   }

   public String getEphemeralId () {

      return ephemeralId;
   }

   public String toString () {

      StringBuilder Builder;

      Builder = new StringBuilder(hostAddress);
      Builder.append(":");
      Builder.append(ephemeralId);

      return Builder.toString();
   }

   public int hashCode () {

      return toString().hashCode();
   }

   public boolean equals (Object obj) {

      if (obj instanceof EphemeralKey) {
         if (obj.toString().equals(this.toString())) {
            return true;
         }
      }
      return false;
   }

   public static EphemeralKey createEphemeralKey (String unparsedKey) {

      String[] keyPartsArray;

      keyPartsArray = unparsedKey.split(":", -1);
      if (keyPartsArray.length == 2) {
         return new EphemeralKey(keyPartsArray[0], keyPartsArray[1]);
      }

      return null;
   }

}
