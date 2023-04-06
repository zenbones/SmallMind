package org.smallmind.mongodb.throng.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface IndexOptions {

  boolean background () default false;

  boolean unique () default false;

  String name () default "";

  boolean sparse () default false;

  long expireAfterSeconds () default 0;

  int version () default 0;

  String weights () default "";

  String defaultLanguage () default "";

  String languageOverride () default "";

  int textVersion () default 0;

  int sphereVersion () default 0;

  int bits () default 0;

  double min () default -360;

  double max () default 360;

  double bucketSize () default 0;

  String storageEngine () default "";

  String partialFilterExpression () default "";

  Collation collation () default @Collation();

  String wildcardProjection () default "";

  boolean hidden () default false;
}
