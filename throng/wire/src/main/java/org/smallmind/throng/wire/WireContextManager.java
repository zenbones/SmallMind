package org.smallmind.throng.wire;

import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;

public class WireContextManager implements PerApplicationDataManager {

  public static void register (String handle, Class<? extends WireContext> contextClass) {

    PerApplicationContext.getPerApplicationData(WireContextManager.class, ConcurrentHashMap.class).put(handle, contextClass);
  }

  public static Class<? extends WireContext> getContextClass (String handle) {

    return (Class<? extends WireContext>)PerApplicationContext.getPerApplicationData(WireContextManager.class, ConcurrentHashMap.class).get(handle);
  }

  static {

    PerApplicationContext.setPerApplicationData(WireContextManager.class, new ConcurrentHashMap<String, Class<? extends WireContext>>());
  }
}
