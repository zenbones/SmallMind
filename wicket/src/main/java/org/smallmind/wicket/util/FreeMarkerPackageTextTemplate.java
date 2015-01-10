/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.wicket.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.wicket.util.template.TextTemplate;
import org.smallmind.nutsnbolts.freemarker.ClassPathTemplateLoader;

public class FreeMarkerPackageTextTemplate extends TextTemplate {

  private static ConcurrentHashMap<Class<?>, Configuration> CONFIG_MAP = new ConcurrentHashMap<>();
  private Template freeMarkerTemplate;

  public FreeMarkerPackageTextTemplate (Class<?> scopeClass, String fileName) {

    Configuration freemarkerConf;

    if ((freemarkerConf = CONFIG_MAP.get(scopeClass)) == null) {
      freemarkerConf = new Configuration();
      freemarkerConf.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);
      freemarkerConf.setTemplateLoader(new ClassPathTemplateLoader(scopeClass, true));

      CONFIG_MAP.put(scopeClass, freemarkerConf);
    }

    try {
      freeMarkerTemplate = freemarkerConf.getTemplate((fileName == null) ? scopeClass.getSimpleName() + ".js" : fileName);
    }
    catch (IOException ioException) {
      throw new RuntimeException(ioException);
    }
  }

  @Override
  public String getString () {

    return freeMarkerTemplate.toString();
  }

  @Override
  public TextTemplate interpolate (Map<String, ?> variables) {

    StringWriter templateWriter = new StringWriter();

    try {
      freeMarkerTemplate.process(variables, templateWriter);
    }
    catch (Exception exception) {
      throw new RuntimeException(exception);
    }

    return new StaticTextTemplate(templateWriter.toString());
  }
}
