package org.smallmind.scribe.pen.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.smallmind.scribe.pen.Level;

@Target ({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention (RetentionPolicy.RUNTIME)
public @interface AutoLog {

   public abstract String name () default "";

   public abstract WithDiscriminator discriminator () default @WithDiscriminator (value = "", ofClass = Unused.class);

   public abstract Level level () default Level.OFF;

   public abstract String title () default "";

   public abstract boolean off () default false;
}
