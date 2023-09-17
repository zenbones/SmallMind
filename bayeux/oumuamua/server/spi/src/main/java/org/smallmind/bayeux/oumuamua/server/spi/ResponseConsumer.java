package org.smallmind.bayeux.oumuamua.server.spi;

import java.util.function.Consumer;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Packet;

@FunctionalInterface
public interface ResponseConsumer<V extends Value<V>> extends Consumer<Packet<V>> {

}
