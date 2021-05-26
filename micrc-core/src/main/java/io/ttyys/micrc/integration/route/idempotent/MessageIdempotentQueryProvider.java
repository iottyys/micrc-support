package io.ttyys.micrc.integration.route.idempotent;

public interface MessageIdempotentQueryProvider {
    String getCreateMessageQuery();
}
