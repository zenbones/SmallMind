package org.smallmind.persistence.database.mongodb;

import com.mongodb.WriteConcern;
import org.springframework.beans.factory.FactoryBean;

public class MorphiaWriteConcern implements FactoryBean<WriteConcern> {

  private MorphiaAcknowledgment acknowledgment;
  private boolean journaled;

  public void setAcknowledgment (MorphiaAcknowledgment acknowledgment) {

    this.acknowledgment = acknowledgment;
  }

  public void setJournaled (boolean journaled) {

    this.journaled = journaled;
  }

  @Override
  public boolean isSingleton () {

    return false;
  }

  @Override
  public Class<?> getObjectType () {

    return WriteConcern.class;
  }

  @Override
  public WriteConcern getObject () {

    WriteConcern writeConcern = acknowledgment.getWriteConcern();

    if (acknowledgment.isJournable()) {
      writeConcern.withJournal(journaled);
    }

    return writeConcern;
  }
}
