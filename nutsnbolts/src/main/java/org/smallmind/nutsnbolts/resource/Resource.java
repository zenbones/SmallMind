package org.smallmind.nutsnbolts.resource;

import java.io.InputStream;

public interface Resource {

   public String getIdentifier ();

   public String getScheme ();

   public String getPath ();

   public void setPath (String path);

   public abstract InputStream getInputStream ()
      throws ResourceException;
}