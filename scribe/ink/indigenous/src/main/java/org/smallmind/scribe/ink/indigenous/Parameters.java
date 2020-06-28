package org.smallmind.scribe.ink.indigenous;

import java.io.Serializable;
import org.smallmind.scribe.pen.adapter.ScribeParameterAdapter;

public class Parameters {

  public void put (String key, Serializable value) {

    ScribeParameterAdapter.getInstance().put(key, value);
  }

  public Serializable get (String key) {

    return ScribeParameterAdapter.getInstance().get(key);
  }

  public void remove (String key) {

    ScribeParameterAdapter.getInstance().remove(key);
  }

  public void clear () {

    ScribeParameterAdapter.getInstance().clear();
  }
}
