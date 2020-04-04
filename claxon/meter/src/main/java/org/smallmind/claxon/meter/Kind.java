package org.smallmind.claxon.meter;

public interface Kind<E extends Enum<E> & Kind<E>> {

  MeterBuilder<?> build (Domain domain, Tag... tags);

  E from (String name);

  E from (Class<?> clazz);
}
