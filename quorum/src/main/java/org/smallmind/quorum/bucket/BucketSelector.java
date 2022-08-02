package org.smallmind.quorum.bucket;

public interface BucketSelector<T> {

  String selection (T input);
}
