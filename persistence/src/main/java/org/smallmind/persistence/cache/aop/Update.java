package org.smallmind.persistence.cache.aop;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target ({})
@Retention (RetentionPolicy.RUNTIME)
public @interface Update {

   public abstract String filter () default "";

   public Vector value ();
}
