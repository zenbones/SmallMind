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
