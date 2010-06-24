package org.smallmind.quorum.transaction.xa;

import javax.transaction.xa.Xid;
import org.smallmind.nutsnbolts.util.Bytes;

public final class SmallMindXid implements Xid {

   public static final int MAXGTRIDSIZE = 1024;
   public static final int MAXBQUALSIZE = 8;

   public static final int SmallMind_FORMAT_ID = 1174;

   private String gtrid;
   private long bqual;

   public SmallMindXid (String gtrid, long bqual) {

      this.gtrid = gtrid;
      this.bqual = bqual;
   }

   protected String getInternalGlobalTransactionId () {

      return gtrid;
   }

   protected long getInternalBranchQualifier () {

      return bqual;
   }

   public int getFormatId () {

      return SmallMind_FORMAT_ID;
   }

   public byte[] getGlobalTransactionId () {

      return gtrid.getBytes();
   }

   public byte[] getBranchQualifier () {

      return Bytes.getBytes(bqual);
   }

   public boolean equals (Object o) {

      if (o instanceof SmallMindXid) {
         if ((((SmallMindXid)o).getInternalGlobalTransactionId().equals(gtrid)) && (((SmallMindXid)o).getInternalBranchQualifier() == bqual)) {
            return true;
         }
      }

      return false;
   }

}
