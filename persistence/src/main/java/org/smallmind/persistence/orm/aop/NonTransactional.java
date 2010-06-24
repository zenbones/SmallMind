package org.smallmind.persistence.orm.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target ({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention (RetentionPolicy.RUNTIME)
public @interface NonTransactional {

   public abstract String[] dataSources () default {};

   public abstract boolean implicit () default true;
}