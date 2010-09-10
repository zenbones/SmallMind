package org.smallmind.wicket.model.freemarker;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.wicket.model.AbstractReadOnlyModel;

public class FreemarkerModel extends AbstractReadOnlyModel<String> {

   private static final Configuration FREEMARKER_CONF;

   public Template template;
   public Map<String, Object> rootMap;

   public FreemarkerModel (String templateText, Map<String, Object> rootMap)
      throws IOException {

      this.rootMap = rootMap;

      template = new Template(null, new StringReader(templateText), FREEMARKER_CONF);
   }

   static {

      FREEMARKER_CONF = new Configuration();
      FREEMARKER_CONF.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);
   }

   @Override
   public String getObject () {

      StringWriter templateWriter = new StringWriter();

      try {
         template.process(rootMap, templateWriter);
      }
      catch (Exception exception) {
         throw new RuntimeException(exception);
      }

      return templateWriter.toString();
   }
}
