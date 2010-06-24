package org.smallmind.scribe.pen.probe;

import java.io.Serializable;
import java.util.Arrays;

public class Correlator implements Serializable {

   private byte[] threadIdentifier;
   private byte[] parentIdentifier;
   private byte[] identifier;
   private int frame;
   private int instance;

   public Correlator (byte[] threadIdentifier, byte[] parentIdentifier, byte[] identifier, int frame, int instance) {

      this.threadIdentifier = threadIdentifier;
      this.parentIdentifier = parentIdentifier;
      this.identifier = identifier;
      this.frame = frame;
      this.instance = instance;
   }

   public byte[] getThreadIdentifier () {
      return threadIdentifier;
   }

   public byte[] getParentIdentifier () {

      return parentIdentifier;
   }

   public byte[] getIdentifier () {

      return identifier;
   }

   public int getFrame () {
      return frame;
   }

   public int getInstance () {

      return instance;
   }

   public int hashCode () {

      return identifier.hashCode();
   }

   public boolean equals (Object obj) {

      return (obj instanceof Correlator) && Arrays.equals(identifier, ((Correlator)obj).getIdentifier());
   }
}