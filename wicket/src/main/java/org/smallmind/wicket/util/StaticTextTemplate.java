package org.smallmind.wicket.util;

import java.util.Map;
import org.apache.wicket.util.template.TextTemplate;

public class StaticTextTemplate extends TextTemplate {

   private String text;

   public StaticTextTemplate (String text) {

      this.text = text;
   }

   @Override
   public String getString () {

      return text;
   }

   @Override
   public TextTemplate interpolate (Map<String, Object> variables) {

      return this;
   }
}
