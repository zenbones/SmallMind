package org.smallmind.scribe.spring;

import java.util.LinkedList;
import java.util.List;
import org.smallmind.scribe.pen.Template;
import org.springframework.beans.factory.InitializingBean;

public class TemplateInitializingBean implements InitializingBean {

   private LinkedList<Template> initialTemplates;

   public TemplateInitializingBean () {

      initialTemplates = new LinkedList<Template>();
   }

   public void setInitialTemplates (List<Template> initialTemplates) {

      this.initialTemplates.addAll(initialTemplates);
   }

   public void afterPropertiesSet ()
      throws Exception {

      for (Template template : initialTemplates) {
         template.register();
      }
   }
}
