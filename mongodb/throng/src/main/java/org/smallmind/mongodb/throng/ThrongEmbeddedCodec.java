package org.smallmind.mongodb.throng;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class ThrongEmbeddedCodec<T> extends ThrongPropertiesCodec<T> {

  public ThrongEmbeddedCodec (Class<T> embeddedClass, ThrongProperties throngProperties) {

    super(embeddedClass, throngProperties);
  }

  @Override
  public T decode (BsonReader reader, DecoderContext decoderContext) {

    T instance;

    reader.readStartDocument();
    instance = super.decode(reader, decoderContext);
    reader.readEndDocument();

    return instance;
  }

  @Override
  public void encode (BsonWriter writer, T value, EncoderContext encoderContext) {

    writer.writeStartDocument();
    super.encode(writer, value, encoderContext);
    writer.writeEndDocument();
  }
}
