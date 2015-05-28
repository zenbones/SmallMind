package org.smallmind.throng.wire;

import java.util.HashMap;

public interface InstanceIdExtractor {

  public abstract String getInstanceId (HashMap<String, Object> argumentMap, WireContext... wireContexts);
}
