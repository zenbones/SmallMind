package org.smallmind.mongodb.throng.query;

import java.util.regex.Pattern;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.smallmind.nutsnbolts.util.MutationUtility;

public class Filter {

  private final String fieldName;
  private Bson bson;

  private Filter (Bson bson) {

    this.bson = bson;

    fieldName = "";
  }

  public Filter (String fieldName) {

    this.fieldName = fieldName;
  }

  public static Filter where (String fieldName) {

    return new Filter(fieldName);
  }

  public static Filter and (Filter... filters) {

    if ((filters == null) || (filters.length == 0)) {

      return new Filter(com.mongodb.client.model.Filters.empty());
    } else {

      return new Filter(com.mongodb.client.model.Filters.and(MutationUtility.toArray(filters, Bson.class, filter -> filter.bson)));
    }
  }

  public static Filter or (Filter... filters) {

    if ((filters == null) || (filters.length == 0)) {

      return new Filter(com.mongodb.client.model.Filters.empty());
    } else {

      return new Filter(com.mongodb.client.model.Filters.or(MutationUtility.toArray(filters, Bson.class, filter -> filter.bson)));
    }
  }

  public Filter eq (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.eq(fieldName, value);

      return this;
    }
  }

  public Filter ne (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.ne(fieldName, value);

      return this;
    }
  }

  public Filter gt (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.gt(fieldName, value);

      return this;
    }
  }

  public Filter gte (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.gte(fieldName, value);

      return this;
    }
  }

  public Filter lt (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.lt(fieldName, value);

      return this;
    }
  }

  public Filter lte (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.lte(fieldName, value);

      return this;
    }
  }

  public Filter exists (boolean exists) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.exists(fieldName, exists);

      return this;
    }
  }

  public Filter in (Object... values) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.in(fieldName, values);

      return this;
    }
  }

  public Filter nin (Object... values) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.nin(fieldName, values);

      return this;
    }
  }

  public Filter regex (Pattern pattern) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.regex(fieldName, pattern);

      return this;
    }
  }

  public Filter not () {

    if (bson == null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.not(bson);

      return this;
    }
  }

  public Bson toBsonDocument (Class<?> documentClass, CodecRegistry codecRegistry) {

    if (bson == null) {
      throw new UnsupportedOperationException();
    } else {

      return bson.toBsonDocument(documentClass, codecRegistry);
    }
  }
}
