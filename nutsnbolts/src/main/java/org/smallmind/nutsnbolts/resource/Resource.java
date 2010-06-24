package org.smallmind.nutsnbolts.resource;

import java.io.InputStream;

public interface Resource {

   public String getId ();

   public abstract InputStream getInputStream ()
      throws ResourceException;

}