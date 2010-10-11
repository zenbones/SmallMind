package org.smallmind.nutsnbolts.lang;

public interface InstanceStaticManager<S> extends StaticManager {

   public abstract void register (S singleton);
}
