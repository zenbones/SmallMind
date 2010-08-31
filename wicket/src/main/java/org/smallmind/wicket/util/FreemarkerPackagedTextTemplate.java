package org.smallmind.wicket.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.wicket.util.template.TextTemplate;
import org.smallmind.nutsnbolts.freemarker.ClassPathTemplateLoader;

public class FreemarkerPackagedTextTemplate extends TextTemplate {

   private static ConcurrentHashMap<Class<?>, Configuration> CONFIG_MAP = new ConcurrentHashMap<Class<?>, Configuration>();

   private Template freemarkerTemplate;

   public FreemarkerPackagedTextTemplate (Class<?> clazz, String fileName) {

      Configuration freemarkerConf;

      if ((freemarkerConf = CONFIG_MAP.get(clazz)) == null) {
         freemarkerConf = new Configuration();
         freemarkerConf.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);
         freemarkerConf.setTemplateLoader(new ClassPathTemplateLoader(clazz));

         CONFIG_MAP.put(clazz, freemarkerConf);
      }

      try {
         freemarkerTemplate = freemarkerConf.getTemplate(fileName);
      }
      catch (IOException ioException) {
         throw new RuntimeException(ioException);
      }
   }

   @Override
   public String getString () {

      return freemarkerTemplate.toString();
   }

   @Override
   public TextTemplate interpolate (Map<String, Object> variables) {

      StringWriter templateWriter = new StringWriter();

      try {
         freemarkerTemplate.process(variables, templateWriter);
      }
      catch (Exception exception) {
         throw new RuntimeException(exception);
      }

      return new StaticTextTemplate(templateWriter.toString());
   }
}
