package org.smallmind.bayeux.oumuamua.api;

import java.util.Set;

public interface Attributed {

  Set<String> getAttributeNames ();

  Object getAttribute (String name);

  void setAttribute (String name, Object value);

  Object removeAttribute (String name);
}
