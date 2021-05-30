package io.ttyys.micrc.integration.route.store;

import org.springframework.integration.store.PriorityCapableChannelMessageStore;
import org.springframework.integration.util.UUIDConverter;

public interface PartitionCapableChannelMessageStore extends PriorityCapableChannelMessageStore {
    String MESSAGE_HEADER_PARTITION_DEFAULT_ID = UUIDConverter.getUUID("NON-PARTITION").toString();
    String MESSAGE_HEADER_PARTITION_KEY = "PARTITION_KEY";
    String MESSAGE_HEADER_PARTITION_ID = "PARTITION_ID";
    String MESSAGE_HEADER_PARTITION_OFFSET = "PARTITION_OFFSET";
    String MESSAGE_HEADER_PARTITION_SEQUENCE = "PARTITION_SEQUENCE";
    String MESSAGE_HEADER_PARTITION_CREATED_AT = "PARTITION_CREATED_AT";
    boolean isPartitionEnabled();
}
