package org.smallmind.nutsnbolts.util;

import java.lang.reflect.Array;
import java.util.List;

public class ListMutator {

  public static <T, U> U[] mutate (List<T> list, Mutation<? super T, U> mutation)
    throws Exception {

    U[] outArray = (U[])Array.newInstance(mutation.getMutatedClass(), list.size());
    int index = 0;

    for (T inType : list) {
      outArray[index++] = mutation.mutate(inType);
    }

    return outArray;
  }
}
