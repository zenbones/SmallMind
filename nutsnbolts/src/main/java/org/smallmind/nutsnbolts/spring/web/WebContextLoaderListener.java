package org.smallmind.nutsnbolts.spring.web;

import javax.servlet.ServletContextEvent;
import org.springframework.web.context.ContextLoaderListener;

public class WebContextLoaderListener extends ContextLoaderListener {

   public void contextInitialized (ServletContextEvent servletContextEvent) {

      super.contextInitialized(servletContextEvent);
   }
}
