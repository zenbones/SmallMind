package org.smallmind.nutsnbolts.resource;

import java.io.InputStream;

public class PlaceholderResource extends AbstractResource {

   public PlaceholderResource (String path) {

      super(path);
   }

   public String getScheme () {

      return "placeholder";
   }

   public InputStream getInputStream () {

      throw new UnsupportedOperationException();
   }
}