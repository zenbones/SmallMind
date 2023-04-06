package org.smallmind.mongodb.throng;

import org.smallmind.mongodb.throng.annotation.Collation;

public class CollationUtility {

  public static com.mongodb.client.model.Collation generate (Collation collationAnnotation) {

    com.mongodb.client.model.Collation.Builder builder = com.mongodb.client.model.Collation.builder();

    builder.backwards(collationAnnotation.backwards());
    builder.caseLevel(collationAnnotation.caseLevel());
    builder.collationAlternate(collationAnnotation.alternate());
    builder.collationCaseFirst(collationAnnotation.caseFirst());
    builder.collationMaxVariable(collationAnnotation.maxVariable());
    builder.collationStrength(collationAnnotation.strength());

    if (!collationAnnotation.locale().isEmpty()) {
      builder.locale(collationAnnotation.locale());
    }

    builder.normalization(collationAnnotation.normalization());
    builder.numericOrdering(collationAnnotation.numericOrdering());

    return builder.build();
  }
}
