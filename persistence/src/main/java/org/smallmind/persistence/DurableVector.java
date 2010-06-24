package org.smallmind.persistence;

import java.util.List;
import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public interface DurableVector<I, D extends Durable<I>> extends Iterable<D> {

   public abstract void add (D durable);

   public abstract void remove (D durable);

   public abstract List<D> asList ();
}