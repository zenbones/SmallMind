package org.smallmind.bayeux.oumuamua.server.spi;

import java.io.IOException;
import java.util.Objects;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Packet;

@FunctionalInterface
public interface ResponseConsumer<V extends Value<V>> {

  void accept (Packet<V> packet)
    throws IOException;

  default ResponseConsumer<V> andThen (ResponseConsumer<V> after) {

    Objects.requireNonNull(after);

    return (Packet<V> packet) -> {
      accept(packet);
      after.accept(packet);
    };
  }
}
