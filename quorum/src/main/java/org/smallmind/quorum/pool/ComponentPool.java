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
package org.smallmind.quorum.pool;

import java.util.LinkedList;

public class ComponentPool<T> {

   private PoolMode poolMode;
   private ComponentFactory<T> componentFactory;
   private LinkedList<T> usedList;
   private LinkedList<T> freeList;
   private int size;

   public ComponentPool (ComponentFactory<T> componentFactory, int size, PoolMode poolMode) {

      this.componentFactory = componentFactory;
      this.size = size;
      this.poolMode = poolMode;

      usedList = new LinkedList<T>();
      freeList = new LinkedList<T>();
   }

   public synchronized T getComponent ()
      throws ComponentPoolException {

      T component = null;

      if (freeList.isEmpty()) {
         if ((usedList.size() < size) || poolMode.equals(PoolMode.EXPANDING_POOL)) {
            try {
               component = componentFactory.createComponent();
            }
            catch (Exception e) {
               throw new ComponentPoolException(e);
            }
         }
         else if (poolMode.equals(PoolMode.BLOCKING_POOL)) {
            try {
               do {
                  wait();

                  if (!freeList.isEmpty()) {
                     component = freeList.remove(0);
                  }
               } while (component == null);
            }
            catch (InterruptedException i) {
               throw new ComponentPoolException(i);
            }
         }
         else {
            throw new ComponentPoolException("Fixed ComponentPool(%s) is completely booked", componentFactory.getClass().getSimpleName());
         }
      }
      else {
         component = freeList.remove(0);
      }

      usedList.add(component);

      return component;
   }

   public synchronized void returnComponent (T component) {

      usedList.remove(component);

      if ((usedList.size() + freeList.size()) < size) {
         freeList.add(component);

         if (poolMode.equals(PoolMode.BLOCKING_POOL)) {
            notify();
         }
      }
   }

   public synchronized int poolSize () {

      return freeList.size() + usedList.size();
   }

   public synchronized int freeSize () {

      return freeList.size();
   }
}
