package org.smallmind.persistence.cache.aop;

import org.smallmind.persistence.Durable;

public @interface Index {

   public abstract Class<? extends Durable> with ();

   public abstract String on () default "id";

   public abstract Class<? extends Comparable> type () default Nothing.class;

   public abstract boolean constant () default false;
}
