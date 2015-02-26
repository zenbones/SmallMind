/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
