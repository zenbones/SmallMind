package org.smallmind.persistence.cache.aop;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.smallmind.persistence.Durable;

@Target ({})
@Retention (RetentionPolicy.RUNTIME)
public @interface Invalidate {

   public abstract Vector vector ();

   public abstract Class<? extends Durable> against ();

   public abstract String filter () default "";
}
