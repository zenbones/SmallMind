package org.smallmind.persistence.cache.aop;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.smallmind.persistence.Durable;

@Target ({})
@Retention (RetentionPolicy.RUNTIME)
public @interface Update {

   public abstract Vector value ();

   public abstract String filter () default "";

   public abstract String onPersist () default "";

   public abstract Proxy proxy () default @Proxy (with = Durable.class, on = "");
}
