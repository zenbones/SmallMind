package org.smallmind.phalanx.wire;

import java.util.HashMap;

public interface InstanceIdExtractor {

  public abstract String getInstanceId (HashMap<String, Object> argumentMap, WireContext... wireContexts);
}
