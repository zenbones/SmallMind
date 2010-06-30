package org.smallmind.persistence.cache.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target (ElementType.TYPE)
@Retention (RetentionPolicy.RUNTIME)
public @interface CachedWith {

   public abstract Update[] updates () default {};

   public abstract Finder[] finders () default {};

   public abstract Invalidate[] invalidators () default {};
}
