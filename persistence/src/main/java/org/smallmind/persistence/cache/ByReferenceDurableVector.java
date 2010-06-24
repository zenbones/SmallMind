package org.smallmind.persistence.cache;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.DurableVector;
import org.terracotta.modules.annotations.AutolockWrite;
import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public class ByReferenceDurableVector<I, D extends Durable<I>> implements DurableVector<I, D> {

   private List<D> elements;

   public ByReferenceDurableVector (List<D> elements) {

      this.elements = elements;
   }

   @AutolockWrite
   public synchronized void add (D durable) {

      if (!elements.contains(durable)) {
         elements.add(durable);
      }
   }

   @AutolockWrite
   public synchronized void remove (D durable) {

      boolean removed;

      do {
         removed = elements.remove(durable);
      } while (removed);
   }

   public List<D> asList () {

      return Collections.unmodifiableList(elements);
   }

   public Iterator<D> iterator () {

      return elements.iterator();
   }
}
