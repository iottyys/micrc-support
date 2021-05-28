package io.ttyys.micrc.integration.route;

import org.springframework.integration.store.PriorityCapableChannelMessageStore;

public interface PartitionCapableChannelMessageStore extends PriorityCapableChannelMessageStore {
    String MESSAGE_HEADER_PARTITION_KEY = "PARTITION_KEY";
    String MESSAGE_HEADER_PARTITION_OFFSET = "PARTITION_OFFSET";
    String MESSAGE_HEADER_SEQUENCE = "PARTITION_SEQUENCE";
    String MESSAGE_HEADER_CREATED_AT = "PARTITION_CREATED_AT";
    String MESSAGE_HEADER_PRIORITY = "PARTITION_PRIORITY";

    boolean isPartitionEnabled();
}
