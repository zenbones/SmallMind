package org.smallmind.scribe.pen.probe;

public class UpdateProbeEntry extends ProbeEntry {

   private long updateTime;
   private int updateCount;

   public UpdateProbeEntry (Probe probe, long updateTime, int updateCount) {

      super(ProbeStatus.UPDATED, probe);

      this.updateTime = updateTime;
      this.updateCount = updateCount;
   }

   public long getUpdateTime () {

      return updateTime;
   }

   public int getUpdateCount () {

      return updateCount;
   }
}