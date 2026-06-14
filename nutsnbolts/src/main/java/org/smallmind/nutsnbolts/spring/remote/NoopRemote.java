package org.smallmind.nutsnbolts.spring.remote;

import java.rmi.Remote;

/**
 * A do-nothing {@link Remote} implementation that exists purely to supply a concrete service entry for the example {@code rmi.xml} configuration, where it stands in as the {@code service} bean wired into a {@link RemoteServiceExporter}.
 */
public class NoopRemote implements Remote {


}
