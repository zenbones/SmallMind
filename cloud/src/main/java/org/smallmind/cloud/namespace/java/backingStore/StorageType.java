package org.smallmind.cloud.namespace.java.backingStore;

public enum StorageType {

   LDAP("ldap");

   private String backingStore;

   private StorageType (String backingStore) {

      this.backingStore = backingStore;
   }

   public String getBackingStore () {

      return backingStore;
   }
}
