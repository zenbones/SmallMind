package org.smallmind.nutsnbolts.resource;

public interface ResourceFactory {

   public abstract ResourceSchemes getValidSchemes ();

   public abstract Resource createResource (String scheme, String path)
      throws ResourceException;
}
