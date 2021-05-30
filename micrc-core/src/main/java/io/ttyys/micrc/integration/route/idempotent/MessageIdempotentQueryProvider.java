package io.ttyys.micrc.integration.route.idempotent;

public interface MessageIdempotentQueryProvider {
    String getCreateMessageQuery();
    String getContainsMessageQuery();

    String getDeleteMessageQuery();

    String getClearGroupMessages();

    String getClearPartitionMessagesQuery();

    String getPartitionPollQuery();
}
