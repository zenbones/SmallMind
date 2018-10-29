package org.smallmind.nutsnbolts.util;

public interface Mutation<T, U> {

  Class<U> getMutatedClass ();

  U mutate (T inType)
    throws Exception;
}
