package org.smallmind.persistence;

public interface Dao<I, P> {

   public abstract String getStatisticsSource ();

   public abstract P get (Class<P> persistentClass, I id);

   public abstract P persist (Class<P> persistentClass, P persistent);

   public abstract void delete (Class<P> persistentClass, P persistent);
}
