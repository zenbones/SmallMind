package org.smallmind.persistence.orm.spring.data.mongo;

import java.io.Serializable;
import com.mongodb.DBObject;
import org.smallmind.persistence.AbstractDurable;
import org.springframework.data.annotation.Id;

public class MongoDataDurable<I extends Serializable & Comparable<I>, D extends MongoDataDurable<I, D>> extends AbstractDurable<I, D> {

  @Id
  private I id;

  @Override
  public I getId () {

    return id;
  }

  @Override
  public void setId (I id) {

    this.id = id;
  }

  public void preSave (final DBObject dbObj) {

    dbObj.removeField("overlayClass");
  }
}
