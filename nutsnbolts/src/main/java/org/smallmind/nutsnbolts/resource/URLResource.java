package org.smallmind.nutsnbolts.resource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class URLResource extends AbstractResource {

   public URLResource (String path) {

      super(path);
   }

   public String getScheme () {

      return "url";
   }

   public InputStream getInputStream ()
      throws IOException {

      return new BufferedInputStream(new URL(getPath()).openStream());
   }
}