package org.smallmind.phalanx.wire;

public @interface Wire {

  Class<? extends WireCodec> codec ();
}
