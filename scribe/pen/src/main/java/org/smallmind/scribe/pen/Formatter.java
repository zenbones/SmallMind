package org.smallmind.scribe.pen;

import java.util.Collection;

public interface Formatter {

   String format (Record record, Collection<Filter> filterCollection);
}
