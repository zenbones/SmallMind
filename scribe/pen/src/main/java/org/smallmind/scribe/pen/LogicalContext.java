package org.smallmind.scribe.pen;

import java.io.Serializable;

public interface LogicalContext extends Serializable {

   public abstract boolean isFilled ();

   public abstract void fillIn ();

   public abstract String getClassName ();

   public abstract String getMethodName ();

   public abstract String getFileName ();

   public abstract boolean isNativeMethod ();

   public abstract int getLineNumber ();
}