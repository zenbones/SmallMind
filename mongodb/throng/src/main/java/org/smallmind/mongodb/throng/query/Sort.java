package org.smallmind.mongodb.throng.query;

import java.util.LinkedList;
import com.mongodb.client.model.Sorts;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

public class Sort {

  private final LinkedList<Bson> sortList = new LinkedList<>();

  public Sort asc (String fieldName) {

    sortList.add(Sorts.ascending(fieldName));

    return this;
  }

  public Sort desc (String fieldName) {

    sortList.add(Sorts.descending(fieldName));

    return this;
  }

  public Bson toBsonDocument (Class<?> documentClass, CodecRegistry codecRegistry) {

    return Sorts.orderBy(sortList).toBsonDocument(documentClass, codecRegistry);
  }
}
