package org.smallmind.persistence.orm;

import java.io.Serializable;
import java.util.List;
import org.smallmind.persistence.Dao;
import org.smallmind.persistence.Durable;

public interface ORMDao<I extends Serializable, D extends Durable<I>> extends Dao<I, D> {

   public abstract Class<D> getManagedClass ();

   public abstract Class<I> getIdClass ();

   public abstract I getId (D object);

   public abstract D detach (D object);

   public abstract D get (I id);

   public abstract D persist (D persistent);

   public abstract void delete (D persistent);

   public abstract List<D> list ();

   public abstract Iterable<D> scroll ();
}
