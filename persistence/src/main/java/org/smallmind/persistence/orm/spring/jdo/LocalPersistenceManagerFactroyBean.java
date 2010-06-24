package org.smallmind.persistence.orm.spring.jdo;

import java.util.Properties;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class LocalPersistenceManagerFactroyBean implements DisposableBean, FactoryBean, InitializingBean {

   private PersistenceManagerFactory persistenceManagerFactory;
   private DataSource dataSource;
   private Properties properties;

   public void setDataSource (DataSource dataSource) {

      this.dataSource = dataSource;
   }

   public void setProperties (Properties properties) {

      this.properties = properties;
   }

   public Object getObject () {

      return persistenceManagerFactory;
   }

   public Class getObjectType () {

      return PersistenceManagerFactory.class;
   }

   public boolean isSingleton () {

      return true;
   }

   public void destroy () {

      persistenceManagerFactory.close();
   }

   public void afterPropertiesSet () {

      persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(properties);
      persistenceManagerFactory.setConnectionFactory(dataSource);
   }
}
