package org.smallmind.cloud.namespace.java;

import javax.naming.Name;
import javax.naming.directory.DirContext;

public class ContextNamePair {

   private DirContext dirContext;
   private Name name;

   public ContextNamePair (DirContext dirContext, Name name) {

      this.dirContext = dirContext;
      this.name = name;
   }

   public DirContext getContext () {

      return dirContext;
   }

   public Name getName () {

      return name;
   }

}
