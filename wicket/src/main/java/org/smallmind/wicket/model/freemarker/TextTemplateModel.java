package org.smallmind.wicket.model.freemarker;

import java.util.Map;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.template.TextTemplate;

public class TextTemplateModel extends AbstractReadOnlyModel<String> {

   private TextTemplate textTemplate;
   private Map<String, Object> variables;

   public TextTemplateModel (TextTemplate textTemplate, Map<String, Object> variables) {

      this.textTemplate = textTemplate;
      this.variables = variables;
   }

   @Override
   public String getObject () {

      return textTemplate.interpolate(variables).getString();
   }
}
