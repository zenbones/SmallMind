package org.smallmind.phalanx.wire.amqp.rabbitmq;

public class NameConfiguration {

  private String requestExchange = "requestExchange";
  private String responseExchange = "responseExchange";
  private String responseQueue = "responseQueue";
  private String talkQueue = "talkQueue";
  private String whisperQueue = "whisperQueue";

  public String getRequestExchange () {

    return requestExchange;
  }

  public void setRequestExchange (String requestExchange) {

    this.requestExchange = requestExchange;
  }

  public String getResponseExchange () {

    return responseExchange;
  }

  public void setResponseExchange (String responseExchange) {

    this.responseExchange = responseExchange;
  }

  public String getResponseQueue () {

    return responseQueue;
  }

  public void setResponseQueue (String responseQueue) {

    this.responseQueue = responseQueue;
  }

  public String getTalkQueue () {

    return talkQueue;
  }

  public void setTalkQueue (String talkQueue) {

    this.talkQueue = talkQueue;
  }

  public String getWhisperQueue () {

    return whisperQueue;
  }

  public void setWhisperQueue (String whisperQueue) {

    this.whisperQueue = whisperQueue;
  }
}
