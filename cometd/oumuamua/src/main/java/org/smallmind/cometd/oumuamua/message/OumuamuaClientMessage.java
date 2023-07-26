package org.smallmind.cometd.oumuamua.message;

import java.util.Map;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Message;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;

public class OumuamuaClientMessage extends MapLike implements Message {

  public OumuamuaClientMessage (ObjectNode node) {

    super(node);
  }

  @Override
  public String getId () {

    Object value;

    return ((value = get(ID_FIELD)) == null) ? null : value.toString();
  }

  @Override
  public String getChannel () {

    Object value;

    return ((value = get(CHANNEL_FIELD)) == null) ? null : value.toString();
  }

  @Override
  public ChannelId getChannelId () {

    Object value;

    return ((value = get(CHANNEL_FIELD)) == null) ? null : ChannelIdCache.generate(value.toString());
  }

  @Override
  public boolean isMeta () {

    ChannelId channelId;

    return ((channelId = getChannelId()) != null) && channelId.isMeta();
  }

  @Override
  public boolean isPublishReply () {

    return !(isMeta() || containsKey(DATA_FIELD));
  }

  @Override
  public boolean isSuccessful () {

    return Boolean.TRUE.equals(get(SUCCESSFUL_FIELD));
  }

  @Override
  public String getClientId () {

    Object value;

    return ((value = get(CLIENT_ID_FIELD)) == null) ? null : value.toString();
  }

  @Override
  public Map<String, Object> getAdvice () {

    return getAsMapLike(ADVICE_FIELD);
  }

  @Override
  public Object getData () {

    return get(DATA_FIELD);
  }

  @Override
  public Map<String, Object> getDataAsMap () {

    return getAsMapLike(DATA_FIELD);
  }

  @Override
  public Map<String, Object> getExt () {

    return getAsMapLike(EXT_FIELD);
  }
}
