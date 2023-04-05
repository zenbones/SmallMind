package org.smallmind.mongodb.throng.mapping;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.smallmind.mongodb.throng.ThrongRuntimeException;

public class ThrongPolymorphicMultiplexerCodec<T> implements Codec<T> {

  private final ThrongPolymorphicMultiplexer<T> polymorphicMultiplexer;

  public ThrongPolymorphicMultiplexerCodec (ThrongPolymorphicMultiplexer<T> polymorphicMultiplexer) {

    this.polymorphicMultiplexer = polymorphicMultiplexer;
  }

  @Override
  public Class<T> getEncoderClass () {

    return polymorphicMultiplexer.getEntityClass();
  }

  @Override
  public T decode (BsonReader reader, DecoderContext decoderContext) {

    String polymorphicKey;

    if (!polymorphicMultiplexer.getKey().equals(polymorphicKey = reader.readName())) {
      throw new ThrongRuntimeException("The expected polymorphic key field(%s) does not match the actual(%s)", polymorphicMultiplexer.getKey(), polymorphicKey);
    } else {

      return polymorphicMultiplexer.getEntityClass().cast(polymorphicMultiplexer.getCodec(reader.readString()).decode(reader, decoderContext));
    }
  }

  @Override
  public void encode (BsonWriter writer, T value, EncoderContext encoderContext) {

    writer.writeName(polymorphicMultiplexer.getKey());
    writer.writeString(value.getClass().getName());

    reEncode(writer, polymorphicMultiplexer.getCodec(value.getClass().getName()), value, encoderContext);
  }

  // Due to the fact that object is not of type 'capture of ?'
  private <U> void reEncode (BsonWriter writer, Codec<U> codec, Object stuff, EncoderContext encoderContext) {

    codec.encode(writer, (U)stuff, encoderContext);
  }
}
