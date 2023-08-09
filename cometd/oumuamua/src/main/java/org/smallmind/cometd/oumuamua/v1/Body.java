package org.smallmind.cometd.oumuamua.v1;

import org.smallmind.cometd.oumuamua.v1.json.ObjectValue;

public abstract class Body extends ObjectValue {

  public abstract String getId ();

  public abstract String getSessionId ();

  public abstract Route getRoute ();

  public abstract ObjectValue getAdvice ();

  public abstract ObjectValue getExt ();

  public abstract ObjectValue getData ();
}
