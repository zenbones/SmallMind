package org.smallmind.mongodb.throng.query;

import java.util.LinkedList;
import com.mongodb.client.model.Projections;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

public class Projection {

  private final LinkedList<Bson> projectionList = new LinkedList<>();

  public Projection include (String... fieldNames) {

    projectionList.add(Projections.include(fieldNames));

    return this;
  }

  public Projection exclude (String... fieldNames) {

    projectionList.add(Projections.exclude(fieldNames));

    return this;
  }

  public Bson toBsonDocument (Class<?> documentClass, CodecRegistry codecRegistry) {

    return Projections.fields(projectionList).toBsonDocument(documentClass, codecRegistry);
  }
}
