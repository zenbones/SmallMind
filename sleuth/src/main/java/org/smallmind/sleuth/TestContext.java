package org.smallmind.sleuth;

import java.util.HashMap;

public class TestContext {

  private static InheritableThreadLocal<HashMap<String, Object>> CONTEXT_MAP = new InheritableThreadLocal<HashMap<String, Object>>() {

    @Override
    protected HashMap<String, Object> initialValue () {

      return new HashMap<>();
    }
  };

  public static Object get (String key) {

    return CONTEXT_MAP.get().get(key);
  }

  public static <T> T get (String key, Class<T> clazz) {

    return clazz.cast(CONTEXT_MAP.get().get(key));
  }

  public static void put (String key, Object value) {

    CONTEXT_MAP.get().put(key, value);
  }

  public static void putIfAbsent (String key, Object value) {

    CONTEXT_MAP.get().putIfAbsent(key, value);
  }
}
