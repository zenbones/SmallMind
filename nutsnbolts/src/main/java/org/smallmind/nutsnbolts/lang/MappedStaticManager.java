package org.smallmind.nutsnbolts.lang;

public interface MappedStaticManager<K,V> extends StaticManager {

   public abstract void register(K key, V value);
}
