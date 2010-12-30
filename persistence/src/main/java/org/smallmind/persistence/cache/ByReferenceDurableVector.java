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
package org.smallmind.persistence.cache;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectorPredicate;
import org.smallmind.persistence.cache.util.CachedList;
import org.terracotta.annotations.AutolockRead;
import org.terracotta.annotations.AutolockWrite;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public class ByReferenceDurableVector<I extends Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

   private CachedList<D> elements;

   public ByReferenceDurableVector (CachedList<D> elements, Comparator<D> comparator, int maxSize, long timeToLive, boolean ordered) {

      super(comparator, maxSize, timeToLive, ordered);

      this.elements = elements;
      if (maxSize > 0) {
         while (elements.size() > maxSize) {
            elements.removeLast();
         }
      }
   }

   @AutolockRead
   public DurableVector<I, D> copy () {

      return new ByReferenceDurableVector<I, D>(new CachedList<D>(elements), getComparator(), getMaxSize(), getTimeToLive(), isOrdered());
   }

   public boolean isSingular () {

      return false;
   }

   @AutolockWrite
   public synchronized void add (D durable) {

      if (durable != null) {

         if (isOrdered()) {

            Iterator<D> elementIter = elements.iterator();
            D element;
            boolean removed = false;
            boolean zoned = false;
            boolean inserted = false;
            int index = 0;

            while ((!(removed && zoned)) && elementIter.hasNext()) {
               element = elementIter.next();

               if (element.equals(durable)) {
                  if (((getComparator() == null) ? element.compareTo(durable) : getComparator().compare(element, durable)) == 0) {
                     zoned = true;
                     inserted = true;
                  }
                  else {
                     elementIter.remove();
                  }

                  removed = true;
               }
               else if (((getComparator() == null) ? element.compareTo(durable) : getComparator().compare(element, durable)) >= 0) {
                  zoned = true;
               }
               else if (!zoned) {
                  index++;
               }
            }

            if (!inserted) {
               elements.add(index, durable);
            }
         }
         else {

            boolean matched = false;

            for (D element : elements) {
               if (element.equals(durable)) {
                  matched = true;
                  break;
               }
            }

            if (!matched) {
               elements.addFirst(durable);
            }
         }

         if ((getMaxSize() > 0) && (elements.size() > getMaxSize())) {
            elements.removeLast();
         }
      }
   }

   @AutolockWrite
   public synchronized void remove (D durable) {

      boolean removed;

      do {
         removed = elements.remove(durable);
      } while (removed);
   }

   @AutolockWrite
   public void removeId (I id) {

      Iterator<D> elementIter = elements.iterator();

      while (elementIter.hasNext()) {
         if (elementIter.next().getId().equals(id)) {
            elementIter.remove();
         }
      }
   }

   @AutolockWrite
   public void filter (VectorPredicate<D> predicate) {

      Iterator<D> elementIter = elements.iterator();

      while (elementIter.hasNext()) {
         if (!predicate.isValid(elementIter.next())) {
            elementIter.remove();
         }
      }
   }

   @AutolockRead
   public synchronized D head () {

      if (elements.isEmpty()) {
         return null;
      }

      return elements.get(0);
   }

   @AutolockRead
   public synchronized List<D> asList () {

      return Collections.unmodifiableList(elements);
   }

   @AutolockRead
   public synchronized Iterator<D> iterator () {

      return Collections.unmodifiableList(elements).iterator();
   }
}
