package org.smallmind.persistence.orm.spring.hibernate;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

public class HibernateFileSeekingFactoryBean implements FactoryBean {

   private String dataSourceKey;

   public void setDataSourceKey (String dataSourceKey) {

      this.dataSourceKey = dataSourceKey;
   }

   public Object getObject () {

      return HibernateFileSeekingBeanFactoryPostProcessor.getHibernateResources(dataSourceKey);
   }

   public Class getObjectType () {

      return Resource[].class;
   }

   public boolean isSingleton () {

      return true;
   }
}
