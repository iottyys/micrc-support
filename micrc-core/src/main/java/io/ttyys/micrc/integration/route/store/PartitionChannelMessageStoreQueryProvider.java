package io.ttyys.micrc.integration.route.store;

import org.springframework.integration.jdbc.store.channel.ChannelMessageStoreQueryProvider;

public interface PartitionChannelMessageStoreQueryProvider extends ChannelMessageStoreQueryProvider {
    String getPartitionOffsetQuery();
    String getCreatePartitionOffsetQuery();
    String getUpdatePartitionOffsetQuery();
}
