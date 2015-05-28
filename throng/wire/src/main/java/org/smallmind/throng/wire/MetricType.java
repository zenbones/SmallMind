package org.smallmind.throng.wire;

public enum MetricType {

  ACQUIRE_REQUEST_DESTINATION("Acquire Request Transport"), ACQUIRE_RESPONSE_DESTINATION("Acquire Response Transport"), CONSTRUCT_MESSAGE("Construct Message"), REQUEST_DESTINATION_TRANSIT("Request Transit"), RESPONSE_TOPIC_TRANSIT("Response Transit"), COMPLETE_CALLBACK("Complete Callback");

  private String display;

  MetricType (String display) {

    this.display = display;
  }

  public String getDisplay () {

    return display;
  }
}