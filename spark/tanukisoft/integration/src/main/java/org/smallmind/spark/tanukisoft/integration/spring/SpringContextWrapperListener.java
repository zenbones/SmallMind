package org.smallmind.spark.tanukisoft.integration.spring;

import org.smallmind.spark.tanukisoft.integration.AbstractWrapperListener;
import org.springframework.context.ConfigurableApplicationContext;

public abstract class SpringContextWrapperListener extends AbstractWrapperListener {

   private ConfigurableApplicationContext[] applicationContexts;

   public abstract ConfigurableApplicationContext[] loadApplicationContexts (String[] args);

   public void startup (String[] args) {

      if (applicationContexts == null) {
         applicationContexts = loadApplicationContexts(args);
      }

      for (ConfigurableApplicationContext applicationContext : applicationContexts) {
         if (!applicationContext.isActive()) {
            applicationContext.refresh();
         }
      }
   }

   public void shutdown () {

      if (applicationContexts != null) {
         for (int count = applicationContexts.length - 1; count >= 0; count--) {
            if (applicationContexts[count].isActive()) {
               applicationContexts[count].close();
            }
         }
      }
   }
}
