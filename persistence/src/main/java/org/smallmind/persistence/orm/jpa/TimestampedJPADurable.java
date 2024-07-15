/*
 * Copyright (c) 2007 through 2024 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm.jpa;

import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.smallmind.nutsnbolts.reflection.FieldAccessor;
import org.smallmind.nutsnbolts.reflection.FieldUtility;
import org.smallmind.persistence.Durable;

@MappedSuperclass
public abstract class TimestampedJPADurable<I extends Serializable & Comparable<I>, D extends TimestampedJPADurable<I, D>> extends JPADurable<I, D> {

  private Date created;
  private Date lastUpdated;

  @CreationTimestamp
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created", nullable = false, updatable = false)
  public synchronized Date getCreated () {

    return created;
  }

  public synchronized D setCreated (Date created) {

    this.created = created;

    return (D)this;
  }

  @UpdateTimestamp
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_updated")
  public synchronized Date getLastUpdated () {

    return lastUpdated;
  }

  public synchronized D setLastUpdated (Date lastUpdated) {

    this.lastUpdated = lastUpdated;

    return (D)this;
  }

  @Override
  public boolean mirrors (Durable durable) {

    FieldAccessor idFieldAccessor = FieldUtility.getFieldAccessor(this.getClass(), "id");
    FieldAccessor createdFieldAccessor = FieldUtility.getFieldAccessor(this.getClass(), "created");
    FieldAccessor lastUpdatedFieldAccessor = FieldUtility.getFieldAccessor(this.getClass(), "lastUpdated");

    return super.mirrors(durable, (idFieldAccessor == null) ? null : idFieldAccessor.getField(), (createdFieldAccessor == null) ? null : createdFieldAccessor.getField(), (lastUpdatedFieldAccessor == null) ? null : lastUpdatedFieldAccessor.getField());
  }
}
