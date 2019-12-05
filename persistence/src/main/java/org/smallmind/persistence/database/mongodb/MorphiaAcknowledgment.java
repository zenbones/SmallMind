package org.smallmind.persistence.database.mongodb;

import com.mongodb.WriteConcern;

public enum MorphiaAcknowledgment {

  ZERO(WriteConcern.UNACKNOWLEDGED, false), ONE(WriteConcern.W1, true), TWO(WriteConcern.W2, true), THREE(WriteConcern.W3, true), MAJORITY(WriteConcern.MAJORITY, true);

  private WriteConcern writeConcern;
  private boolean journable;

  MorphiaAcknowledgment (WriteConcern writeConcern, boolean journable) {

    this.writeConcern = writeConcern;
    this.journable = journable;
  }

  public WriteConcern getWriteConcern () {

    return writeConcern;
  }

  public boolean isJournable () {

    return journable;
  }
}

