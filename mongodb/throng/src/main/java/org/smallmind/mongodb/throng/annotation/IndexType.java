package org.smallmind.mongodb.throng.annotation;

import com.mongodb.client.model.Indexes;
import org.bson.conversions.Bson;

public enum IndexType {

  ASCENDING {
    @Override
    public Bson construct (String field) {

      return Indexes.ascending(field);
    }
  },
  DESCENDING {
    @Override
    public Bson construct (String field) {

      return Indexes.descending(field);
    }
  },
  HASHED {
    @Override
    public Bson construct (String field) {

      return Indexes.hashed(field);
    }
  },
  TEXT {
    @Override
    public Bson construct (String field) {

      return Indexes.text(field);
    }
  },
  GEO2D {
    @Override
    public Bson construct (String field) {

      return Indexes.geo2d(field);
    }
  },
  GEO2DSPHERE {
    @Override
    public Bson construct (String field) {

      return Indexes.geo2dsphere(field);
    }
  };

  public abstract Bson construct (String field);
}
