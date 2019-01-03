package org.smallmind.batching.spring;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;

public class BatchProxyStep implements Step {

  @Override
  public String getName () {

    return null;
  }

  @Override
  public boolean isAllowStartIfComplete () {

    return false;
  }

  @Override
  public int getStartLimit () {

    return 0;
  }

  @Override
  public void execute (StepExecution stepExecution) {

  }
}
