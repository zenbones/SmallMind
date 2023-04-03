package org.smallmind.mongodb.throng;

import org.smallmind.nutsnbolts.reflection.FieldAccessor;

public class ThrongProperty {

  private final FieldAccessor fieldAccessor;
  private final org.bson.codecs.Codec<?> codec;
  private final String name;

  public ThrongProperty (FieldAccessor fieldAccessor, org.bson.codecs.Codec<?> codec, String name) {

    this.fieldAccessor = fieldAccessor;
    this.codec = codec;
    this.name = name;
  }

  public FieldAccessor getFieldAccessor () {

    return fieldAccessor;
  }

  public org.bson.codecs.Codec<?> getCodec () {

    return codec;
  }

  public String getName () {

    return name;
  }
}
