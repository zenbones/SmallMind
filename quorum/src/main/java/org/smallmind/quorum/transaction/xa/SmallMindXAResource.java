package org.smallmind.quorum.transaction.xa;

import javax.transaction.xa.XAResource;

public interface SmallMindXAResource extends XAResource {

   public abstract Object getResource ()
      throws org.smallmind.quorum.pool.ConnectionPoolException;

}
