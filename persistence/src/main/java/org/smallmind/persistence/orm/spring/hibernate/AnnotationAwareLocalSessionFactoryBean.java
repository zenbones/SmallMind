package org.smallmind.persistence.orm.spring.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

public class AnnotationAwareLocalSessionFactoryBean extends LocalSessionFactoryBean {

   private String dataSourceKey;

   public void setDataSourceKey (String dataSourceKey) {

      this.dataSourceKey = dataSourceKey;
   }

   @Override
   protected Configuration newConfiguration ()
      throws HibernateException {

      AnnotationConfiguration configuration = new AnnotationConfiguration();

      for (Class annotatedClass : HibernateAnnotationSeekingBeanFactoryPostProcessor.getAnnotatedClasses(dataSourceKey)) {
         configuration.addAnnotatedClass(annotatedClass);
      }

      return configuration;
   }
}
