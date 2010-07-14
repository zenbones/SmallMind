package org.smallmind.nutsnbolts.util;

public enum SystemPropertyMode {

   NEVER, // If no config property is set, none will be
   FALLBACK, // If no config property is set, fallback to the system property, or, if none, the env property (assuming searching the env is allowed)
   OVERRIDE // Take the system property first, or, if none, the env property (assuming searching the env is allowed)
}
