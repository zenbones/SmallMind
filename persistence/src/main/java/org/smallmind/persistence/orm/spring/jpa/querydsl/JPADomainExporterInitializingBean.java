package org.smallmind.persistence.orm.spring.jpa.querydsl;

import java.io.File;
import javax.persistence.EntityManagerFactory;
import com.mysema.query.jpa.codegen.JPADomainExporter;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;

public class JPADomainExporterInitializingBean implements InitializingBean {

  private SessionFactory sessionFactory;
  private EntityManagerFactory entityManagerFactory;
  private File targetPath;
  private String prefix;

  public void setEntityManagerFactory (EntityManagerFactory entityManagerFactory) {

    this.entityManagerFactory = entityManagerFactory;
  }

  public void setTargetPath (File targetPath) {

    this.targetPath = targetPath;
  }

  public void setPrefix (String prefix) {

    this.prefix = prefix;
  }

  @Override
  public void afterPropertiesSet () throws Exception {

    JPADomainExporter jpaDomainExporter = new JPADomainExporter(prefix, targetPath, null);
  }
}
