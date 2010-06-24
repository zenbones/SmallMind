package org.smallmind.nutsnbolts.resource;

import java.io.OutputStream;

public interface MutableResource extends Resource {

   public abstract OutputStream getOutputStream ()
      throws ResourceException;

}
