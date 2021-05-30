package io.ttyys.micrc.integration.route.idempotent;

public class H2MessageIdempotentQueryProvider implements MessageIdempotentQueryProvider {
    @Override
    public String getCreateMessageQuery() {
        return null;
    }

    @Override
    public String getContainsMessageQuery() {
        return null;
    }

    @Override
    public String getDeleteMessageQuery() {
        return null;
    }

    @Override
    public String getClearGroupMessages() {
        return null;
    }

    @Override
    public String getClearPartitionMessagesQuery() {
        return null;
    }

    @Override
    public String getPartitionPollQuery() {
        return null;
    }
}
