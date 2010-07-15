package org.smallmind.nutsnbolts.spring;

import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.nutsnbolts.resource.ResourceParser;
import org.smallmind.nutsnbolts.resource.ResourceTypeFactory;
import org.springframework.core.convert.converter.Converter;

public class ResourceConverter implements Converter<String, Resource> {

   private static final ResourceParser RESOURCE_PARSER = new ResourceParser(new ResourceTypeFactory());

   public Resource convert (String s) {

      try {

         return RESOURCE_PARSER.parseResource(s);
      }
      catch (ResourceException resourceException) {
         throw new RuntimeBeansException(resourceException);
      }
   }
}
