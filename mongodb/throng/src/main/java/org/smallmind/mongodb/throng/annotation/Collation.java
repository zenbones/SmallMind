package org.smallmind.mongodb.throng.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Collation {

  String locale () default "";

  boolean caseLevel () default false;

  CollationCaseFirst caseFirst () default CollationCaseFirst.OFF;

  CollationStrength strength () default CollationStrength.TERTIARY;

  boolean numericOrdering () default false;

  CollationAlternate alternate () default CollationAlternate.NON_IGNORABLE;

  CollationMaxVariable maxVariable () default CollationMaxVariable.SPACE;

  boolean normalization () default false;

  boolean backwards () default false;
}
