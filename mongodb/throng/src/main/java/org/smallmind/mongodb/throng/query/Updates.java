package org.smallmind.mongodb.throng.query;

import java.util.LinkedList;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

public class Updates {

  private final LinkedList<Bson> updateList = new LinkedList<>();

  public Updates set (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.set(fieldName, value));

    return this;
  }

  public Updates unset (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.unset(fieldName));

    return this;
  }

  public Updates setOnInsert (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.setOnInsert(fieldName, value));

    return this;
  }

  public Updates inc (String fieldName, Number number) {

    updateList.add(com.mongodb.client.model.Updates.inc(fieldName, number));

    return this;
  }

  public Updates mul (String fieldName, Number number) {

    updateList.add(com.mongodb.client.model.Updates.mul(fieldName, number));

    return this;
  }

  public Updates max (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.max(fieldName, value));

    return this;
  }

  public Updates min (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.min(fieldName, value));

    return this;
  }

  public Updates push (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.push(fieldName, value));

    return this;
  }

  public Updates addToSet (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.addToSet(fieldName, value));

    return this;
  }

  public Updates pull (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.pull(fieldName, value));

    return this;
  }

  public Updates popFirst (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.popFirst(fieldName));

    return this;
  }

  public Updates popLast (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.popLast(fieldName));

    return this;
  }

  public Updates rename (String fieldName, String updatedFieldName) {

    updateList.add(com.mongodb.client.model.Updates.rename(fieldName, updatedFieldName));

    return this;
  }

  public Bson toBsonDocument (Class<?> documentClass, CodecRegistry codecRegistry) {

    return com.mongodb.client.model.Updates.combine(updateList).toBsonDocument(documentClass, codecRegistry);
  }
}
