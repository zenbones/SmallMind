package org.smallmind.persistence.cache.aop;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target ({})
@Retention (RetentionPolicy.RUNTIME)
public @interface Vector {

   public abstract Index[] value ();

   public abstract String classifier () default "";

   public abstract boolean asParameter () default false;
}
