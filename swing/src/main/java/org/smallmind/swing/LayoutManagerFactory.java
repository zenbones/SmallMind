/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.swing;

import java.awt.LayoutManager;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;

public class LayoutManagerFactory {

   private static final Class[] NO_SIGNATURE = new Class[0];
   private static final Object[] NO_PARAMETERS = new Object[0];
   private static final HashMap<InstanceKey, LayoutManager> LAYOUT_MANAGER_MAP = new HashMap<InstanceKey, LayoutManager>();

   public static LayoutManager getLayoutManager (Class<? extends LayoutManager> layoutManagerClass)
      throws LayoutManagerConstructionException {

      return getLayoutManager(layoutManagerClass, NO_SIGNATURE, NO_PARAMETERS);
   }

   public static LayoutManager getLayoutManager (Class<? extends LayoutManager> layoutManagerClass, Class[] signature, Object[] parameters)
      throws LayoutManagerConstructionException {

      LayoutManager layoutManager;
      Constructor<? extends LayoutManager> layoutManagerConstructor;
      InstanceKey instanceKey;

      instanceKey = new LayoutManagerFactory.InstanceKey(layoutManagerClass, signature, parameters);
      if ((layoutManager = LAYOUT_MANAGER_MAP.get(instanceKey)) == null) {
         try {
            layoutManagerConstructor = layoutManagerClass.getConstructor(signature);
            layoutManager = layoutManagerConstructor.newInstance(parameters);
         }
         catch (Exception e) {
            throw new LayoutManagerConstructionException(e);
         }

         LAYOUT_MANAGER_MAP.put(instanceKey, layoutManager);
      }

      return layoutManager;
   }

   private static class InstanceKey {

      private Class layoutManagerClass;
      private Class[] signature;
      private Object[] parameters;

      public InstanceKey (Class layoutManagerClass, Class[] signature, Object[] parameters) {

         this.layoutManagerClass = layoutManagerClass;
         this.signature = signature;
         this.parameters = parameters;
      }

      public Class getLayoutManagerClass () {

         return layoutManagerClass;
      }

      public Class[] getSignature () {

         return signature;
      }

      public Object[] getParameters () {

         return parameters;
      }

      public int hashCode () {

         return layoutManagerClass.hashCode();
      }

      public boolean equals (Object obj) {

         if (obj instanceof InstanceKey) {
            if (layoutManagerClass.equals(((InstanceKey)obj).getLayoutManagerClass())) {
               if (Arrays.equals(signature, ((InstanceKey)obj).getSignature())) {
                  if (Arrays.equals(parameters, ((InstanceKey)obj).getParameters())) {
                     return true;
                  }
               }
            }
         }

         return false;
      }

   }

}
