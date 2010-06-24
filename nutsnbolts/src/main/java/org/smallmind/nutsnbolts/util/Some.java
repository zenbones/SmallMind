package org.smallmind.nutsnbolts.util;

import com.bizzy.util.Option;

public class Some<T> implements Option<T> {

  private T value;

  public Some (T value) {

    this.value = value;
  }

  public boolean isNone () {

    return false;
  }

  public T get () {

    return value;
  }
}
