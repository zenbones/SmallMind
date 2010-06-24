package org.smallmind.nutsnbolts.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target ({ElementType.TYPE})
@Retention (RetentionPolicy.RUNTIME)
public @interface ExpectedContexts {

   Class<? extends Context>[] value ();
}
