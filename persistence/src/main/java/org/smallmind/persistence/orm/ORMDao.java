package org.smallmind.persistence.orm;

import java.io.Serializable;
import java.util.List;
import org.smallmind.persistence.Dao;
import org.smallmind.persistence.Durable;

public interface ORMDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends Dao<I, D> {

   public abstract String getDataSource ();

   public abstract ProxySession getSession ();

   public abstract Class<D> getManagedClass ();

   public abstract Class<I> getIdClass ();

   public abstract I getIdFromString (String value);

   public abstract I getId (D durable);

   public abstract D detach (D durable);

   public abstract D get (I id);

   public abstract D persist (D durable);

   public abstract void delete (D durable);

   public abstract List<D> list ();

   public abstract Iterable<D> scroll (int fetchSize);

   public abstract Iterable<D> scrollById (I greaterThan, int fetchSize);

   public abstract long size ();
}