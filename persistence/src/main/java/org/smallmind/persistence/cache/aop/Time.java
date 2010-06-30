package org.smallmind.persistence.cache.aop;

import java.util.concurrent.TimeUnit;

public @interface Time {

   public abstract long value ();

   public abstract int stochastic () default 0;

   public abstract TimeUnit unit () default TimeUnit.MILLISECONDS;
}
