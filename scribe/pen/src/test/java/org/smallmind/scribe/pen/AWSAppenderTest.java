/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.pen;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogStream;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.RejectedLogEventsInfo;

@Test(groups = "unit")
public class AWSAppenderTest {

  private CloudWatchLogsClient mockClientWithStream (String uploadSequenceToken) {

    CloudWatchLogsClient client = Mockito.mock(CloudWatchLogsClient.class);
    DescribeLogStreamsResponse describeResponse = DescribeLogStreamsResponse.builder().logStreams(LogStream.builder().logStreamName("stream").uploadSequenceToken(uploadSequenceToken).build()).build();

    Mockito.when(client.describeLogStreams(Mockito.any(DescribeLogStreamsRequest.class))).thenReturn(describeResponse);

    return client;
  }

  public void testConstructorSeedsSequenceTokenFromDescribedStream () {

    CloudWatchLogsClient client = mockClientWithStream("token-0");

    AWSAppender appender = new AWSAppender(new PatternFormatter("%m"), null, client, "group", "stream");

    Assert.assertNotNull(appender);
  }

  @Test(expectedExceptions = LoggerRuntimeException.class)
  public void testConstructorThrowsWhenStreamDoesNotExist () {

    CloudWatchLogsClient client = Mockito.mock(CloudWatchLogsClient.class);

    Mockito.when(client.describeLogStreams(Mockito.any(DescribeLogStreamsRequest.class))).thenReturn(DescribeLogStreamsResponse.builder().build());

    new AWSAppender(new PatternFormatter("%m"), null, client, "group", "stream");
  }

  public void testHandleOutputPutsEventsAndAdvancesSequenceToken ()
    throws Exception {

    CloudWatchLogsClient client = mockClientWithStream("token-0");

    Mockito.when(client.putLogEvents(Mockito.any(PutLogEventsRequest.class))).thenReturn(PutLogEventsResponse.builder().nextSequenceToken("token-1").build());

    AWSAppender appender = new AWSAppender(new PatternFormatter("%m"), null, client, "group", "stream");

    appender.handleOutput(new RecordFixture().setLevel(Level.INFO).setMessage("first").setMillis(System.currentTimeMillis()));
    appender.handleOutput(new RecordFixture().setLevel(Level.INFO).setMessage("second").setMillis(System.currentTimeMillis()));

    ArgumentCaptor<PutLogEventsRequest> requestCaptor = ArgumentCaptor.forClass(PutLogEventsRequest.class);

    Mockito.verify(client, Mockito.times(2)).putLogEvents(requestCaptor.capture());

    // The first call uses the seeded token; after a successful response the appender advances to the
    // token returned by the prior put, proving the sequence-token bookkeeping is wired through.
    Assert.assertEquals(requestCaptor.getAllValues().get(0).sequenceToken(), "token-0");
    Assert.assertEquals(requestCaptor.getAllValues().get(1).sequenceToken(), "token-1");
  }

  public void testNullStreamNameCreatesAStream () {

    CloudWatchLogsClient client = mockClientWithStream("token-0");

    Mockito.when(client.createLogStream(Mockito.any(CreateLogStreamRequest.class))).thenReturn(CreateLogStreamResponse.builder().build());

    new AWSAppender(new PatternFormatter("%m"), null, client, "group", null);

    Mockito.verify(client, Mockito.times(1)).createLogStream(Mockito.any(CreateLogStreamRequest.class));
  }

  @Test(expectedExceptions = LoggerRuntimeException.class)
  public void testConstructorThrowsWhenStreamIsNotUnique () {

    CloudWatchLogsClient client = Mockito.mock(CloudWatchLogsClient.class);
    DescribeLogStreamsResponse describeResponse = DescribeLogStreamsResponse.builder().logStreams(LogStream.builder().logStreamName("stream-a").build(), LogStream.builder().logStreamName("stream-b").build()).build();

    Mockito.when(client.describeLogStreams(Mockito.any(DescribeLogStreamsRequest.class))).thenReturn(describeResponse);

    new AWSAppender(new PatternFormatter("%m"), null, client, "group", "stream");
  }

  @Test(expectedExceptions = LoggerRuntimeException.class)
  public void testExpiredRejectionThrows ()
    throws Exception {

    appenderRejecting(RejectedLogEventsInfo.builder().expiredLogEventEndIndex(0).build()).handleOutput(new RecordFixture().setLevel(Level.INFO).setMessage("expired").setMillis(System.currentTimeMillis()));
  }

  @Test(expectedExceptions = LoggerRuntimeException.class)
  public void testTooNewRejectionThrows ()
    throws Exception {

    appenderRejecting(RejectedLogEventsInfo.builder().tooNewLogEventStartIndex(0).build()).handleOutput(new RecordFixture().setLevel(Level.INFO).setMessage("too-new").setMillis(System.currentTimeMillis()));
  }

  @Test(expectedExceptions = LoggerRuntimeException.class)
  public void testTooOldRejectionThrows ()
    throws Exception {

    appenderRejecting(RejectedLogEventsInfo.builder().tooOldLogEventEndIndex(0).build()).handleOutput(new RecordFixture().setLevel(Level.INFO).setMessage("too-old").setMillis(System.currentTimeMillis()));
  }

  private AWSAppender appenderRejecting (RejectedLogEventsInfo rejectedLogEventsInfo) {

    CloudWatchLogsClient client = mockClientWithStream("token-0");

    Mockito.when(client.putLogEvents(Mockito.any(PutLogEventsRequest.class))).thenReturn(PutLogEventsResponse.builder().rejectedLogEventsInfo(rejectedLogEventsInfo).build());

    return new AWSAppender(new PatternFormatter("%m"), null, client, "group", "stream");
  }
}
