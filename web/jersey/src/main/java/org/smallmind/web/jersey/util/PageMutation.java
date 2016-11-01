package org.smallmind.web.jersey.util;

public interface PageMutation<T, U> {

  public abstract Class<U> getMutatedClass ();

  public abstract U mutate (T inType)
    throws Exception;
}
