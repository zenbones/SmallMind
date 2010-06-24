package org.smallmind.persistence.orm.hibernate;

import org.hibernate.Criteria;

public abstract class CriteriaDetails {

   private String alias;

   public CriteriaDetails () {

      this(null);
   }

   public CriteriaDetails (String alias) {

      this.alias = alias;
   }

   public String getAlias () {

      return alias;
   }

   public abstract Criteria completeCriteria (Criteria criteria);
}
