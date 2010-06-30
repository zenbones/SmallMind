package org.smallmind.persistence.cache.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

@Target (ElementType.METHOD)
@Retention (RetentionPolicy.RUNTIME)
public @interface CacheAs {

   public abstract Vector value ();

   public abstract Class<? extends Comparator> comparator () default Comparator.class;

   public abstract int max () default 0;

   public abstract Time time () default @Time (0);

   public abstract boolean ordered () default false;

   public abstract boolean singular () default false;
}
