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

import java.util.List;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogStream;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.RejectedLogEventsInfo;

/**
 * Appender that publishes formatted log records to an AWS CloudWatch Logs stream using the AWS SDK.
 * Sequence-token ordering is enforced by synchronizing all {@code PutLogEvents} calls; therefore this
 * appender must be used as a singleton per JVM per log stream so that tokens remain serialized.
 */
public class AWSAppender extends AbstractAppender {

  private final CloudWatchLogsClient client;
  private final Formatter formatter;
  private final String groupName;
  private final String streamName;
  private String sequenceToken;

  /**
   * Constructs an appender that publishes to the specified CloudWatch Logs group and stream.
   * If {@code streamName} is {@code null}, a new stream with a Snowflake-generated hex name is created
   * automatically. The constructor verifies that the target stream exists and is unique within the group,
   * and seeds the internal sequence token from the stream descriptor. A {@link LoggerRuntimeException}
   * is thrown immediately if the stream does not exist or is not unique.
   *
   * @param formatter    the formatter used to convert log records to strings before publishing
   * @param errorHandler the handler invoked when publishing fails, or {@code null} to discard errors
   * @param client       a fully configured {@link CloudWatchLogsClient} used for all API calls
   * @param groupName    the name of the CloudWatch Logs log group
   * @param streamName   the name of the log stream within the group, or {@code null} to auto-create one
   * @throws LoggerRuntimeException if the target stream does not exist or the stream name is not unique
   */
  public AWSAppender (Formatter formatter, ErrorHandler errorHandler, CloudWatchLogsClient client, String groupName, String streamName) {

    super(errorHandler);

    List<LogStream> describedLogStreamList;

    this.formatter = formatter;
    this.client = client;
    this.groupName = groupName;
    this.streamName = streamName;

    if (streamName == null) {
      client.createLogStream(CreateLogStreamRequest.builder().logGroupName(groupName).logStreamName(streamName = SnowflakeId.newInstance().generateHexEncoding()).build());
    }

    describedLogStreamList = client.describeLogStreams(DescribeLogStreamsRequest.builder().logGroupName(groupName).logStreamNamePrefix(streamName).build()).logStreams();
    if ((describedLogStreamList == null) || describedLogStreamList.isEmpty()) {
      throw new LoggerRuntimeException("The log stream(groupName=%s, streamName=%s) does not exist", groupName, streamName);
    } else if (describedLogStreamList.size() > 1) {
      throw new LoggerRuntimeException("The log stream(groupName=%s, streamName=%s) is not unique", groupName, streamName);
    } else {
      sequenceToken = describedLogStreamList.get(0).uploadSequenceToken();
    }
  }

  /**
   * Formats the record and submits it to CloudWatch Logs as a single {@code PutLogEvents} request.
   * The method is synchronized to preserve the sequence token ordering that the service requires;
   * on a successful response the internal token is advanced to the next value returned by the API.
   * If the service rejects the event (expired, too old, or too new), a {@link LoggerRuntimeException}
   * is thrown describing the rejection reason.
   *
   * @param record the log record to format and publish
   * @throws Exception if the formatter raises an exception, if the CloudWatch Logs API call fails,
   *                   or if the submitted event is rejected by the service
   */
  @Override
  public synchronized void handleOutput (Record<?> record)
    throws Exception {

    InputLogEvent inputLogEvent = InputLogEvent.builder().message(formatter.format(record)).timestamp(record.getMillis()).build();
    PutLogEventsRequest putLogEventsRequest = PutLogEventsRequest.builder().logGroupName(groupName).logStreamName(streamName).sequenceToken(sequenceToken).logEvents(inputLogEvent).build();
    PutLogEventsResponse putLogEventsResponse = client.putLogEvents(putLogEventsRequest);
    RejectedLogEventsInfo rejectedLogEventsInfo;

    if ((rejectedLogEventsInfo = putLogEventsResponse.rejectedLogEventsInfo()) != null) {
      if (rejectedLogEventsInfo.expiredLogEventEndIndex() != null) {
        throw new LoggerRuntimeException("Log entry(%s) has expired", sequenceToken);
      } else if (rejectedLogEventsInfo.tooNewLogEventStartIndex() != null) {
        throw new LoggerRuntimeException("Log entry(%s) is too new", sequenceToken);
      } else if (rejectedLogEventsInfo.tooOldLogEventEndIndex() != null) {
        throw new LoggerRuntimeException("Log entry(%s) is too old", sequenceToken);
      }
    } else {
      sequenceToken = putLogEventsResponse.nextSequenceToken();
    }
  }
}
