package org.smallmind.quorum.bucket;

public interface BucketFactory<T> {

  TokenBucket<T> create ();
}
