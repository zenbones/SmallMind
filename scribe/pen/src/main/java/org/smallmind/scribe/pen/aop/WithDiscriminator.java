package org.smallmind.scribe.pen.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.smallmind.scribe.pen.Discriminator;

@Target (ElementType.PARAMETER)
@Retention (RetentionPolicy.RUNTIME)
public @interface WithDiscriminator {

   public abstract String value ();

   public abstract Class<? extends Discriminator> ofClass ();
}
