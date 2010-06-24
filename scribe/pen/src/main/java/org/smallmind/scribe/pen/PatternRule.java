package org.smallmind.scribe.pen;

import java.util.Collection;

public interface PatternRule {

   String getHeader ();

   String getFooter ();

   String convert (Record record, Collection<Filter> filterCollection, Timestamp timestamp);
}