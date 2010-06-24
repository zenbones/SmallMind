package org.smallmind.persistence.cache.aop;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.smallmind.persistence.Durable;

@Target ({})
@Retention (RetentionPolicy.RUNTIME)
public @interface Vector {

   public abstract Class<? extends Durable> with ();

   public abstract String on ();

   public String classifier () default "";
}
