package io.ttyys.micrc.integration.route.store;

import org.springframework.integration.jdbc.store.channel.AbstractChannelMessageStoreQueryProvider;

public class MySqlChannelMessageStoreQueryProvider extends AbstractChannelMessageStoreQueryProvider
                implements PartitionChannelMessageStoreQueryProvider {

        private static final String SELECT_COMMON =
                        "SELECT %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID, %PREFIX%CHANNEL_MESSAGE.MESSAGE_BYTES "
                        + "from %PREFIX%CHANNEL_MESSAGE "
                        + "where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key and %PREFIX%CHANNEL_MESSAGE.REGION = :region ";

        @Override
        public String getPollFromGroupExcludeIdsQuery() {
                return SELECT_COMMON
                                + "and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) "
                                + "order by CREATED_DATE, MESSAGE_SEQUENCE LIMIT 1 FOR UPDATE NOWAIT";
        }

        @Override
        public String getPollFromGroupQuery() {
                return SELECT_COMMON +
                                "order by CREATED_DATE, MESSAGE_SEQUENCE LIMIT 1 FOR UPDATE NOWAIT";
        }

        @Override
        public String getPriorityPollFromGroupExcludeIdsQuery() {
                return SELECT_COMMON +
                                "and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) " +
                                "order by MESSAGE_PRIORITY DESC, CREATED_DATE, MESSAGE_SEQUENCE LIMIT 1 FOR UPDATE NOWAIT";
        }

        @Override
        public String getPriorityPollFromGroupQuery() {
                return SELECT_COMMON +
                                "order by MESSAGE_PRIORITY DESC, CREATED_DATE, MESSAGE_SEQUENCE LIMIT 1 FOR UPDATE NOWAIT";
        }

        @Override
        public String getPartitionOffsetQuery() {
                return "SELECT %PREFIX%CHANNEL_MESSAGE_PARTITION.PARTITION_SEQUENCE " +
                                "FROM %PREFIX%CHANNEL_MESSAGE_PARTITION " +
                                "WHERE %PREFIX%CHANNEL_MESSAGE_PARTITION.GROUP_KEY = :group_key " +
                                        "AND %PREFIX%CHANNEL_MESSAGE_PARTITION.REGION = :region " +
                                        "AND %PREFIX%CHANNEL_MESSAGE_PARTITION.PARTITION_KEY = :partition_key " +
                                "FOR UPDATE NOWAIT";
        }

        @Override
        public String getCreatePartitionOffsetQuery() {
                return "INSERT INTO %PREFIX%CHANNEL_MESSAGE_PARTITION(GROUP_KEY, REGION, PARTITION_KEY, PARTITION_SEQUENCE) " +
                                "VALUES(:group_key, :region, :partition_key, :partition_sequence)";
        }

        @Override
        public String getUpdatePartitionOffsetQuery() {
                return "UPDATE FROM %PREFIX%CHANNEL_MESSAGE_PARTITION " +
                                "SET PARTITION_SEQUENCE=:partition_sequence " +
                                "WHERE %PREFIX%CHANNEL_MESSAGE_PARTITION.GROUP_KEY = :group_key " +
                                        "AND %PREFIX%CHANNEL_MESSAGE_PARTITION.REGION = :region " +
                                        "AND %PREFIX%CHANNEL_MESSAGE_PARTITION.PARTITION_KEY = :partition_key ";
        }
}
