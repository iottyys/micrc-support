package io.ttyys.micrc.integration.route.store;

import org.springframework.integration.jdbc.store.channel.AbstractChannelMessageStoreQueryProvider;

public class H2ChannelMessageStoreQueryProvider extends AbstractChannelMessageStoreQueryProvider
		implements PartitionChannelMessageStoreQueryProvider {

	private static final String SELECT_COMMON =
			"SELECT %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID, %PREFIX%CHANNEL_MESSAGE.MESSAGE_BYTES, " +
					"%PREFIX%CHANNEL_MESSAGE.MESSAGE_SEQUENCE, %PREFIX%CHANNEL_MESSAGE.CREATED_DATE " +
					"FROM %PREFIX%CHANNEL_MESSAGE " +
					"WHERE %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key " +
						"AND %PREFIX%CHANNEL_MESSAGE.REGION = :region ";

	private static final String LOCK_SUFFIX = "FOR UPDATE NOWAIT";

	@Override
	public String getCreateMessageQuery() {
		return "INSERT INTO %PREFIX%CHANNEL_MESSAGE(MESSAGE_ID, GROUP_KEY, REGION, CREATED_DATE, MESSAGE_PRIORITY, " +
					"MESSAGE_SEQUENCE, MESSAGE_BYTES) " +
				"VALUES (?, ?, ?, ?, ?, NEXT VALUE FOR %PREFIX%MESSAGE_SEQ, ?)";
	}

	@Override
	public String getPollFromGroupExcludeIdsQuery() {
		return SELECT_COMMON +
				"AND %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) " +
				"ORDER BY CREATED_DATE, MESSAGE_SEQUENCE " +
				"LIMIT 1 " +
				LOCK_SUFFIX;
	}

	@Override
	public String getPollFromGroupQuery() {
		return SELECT_COMMON +
				"ORDER BY CREATED_DATE, MESSAGE_SEQUENCE " +
				"LIMIT 1 " +
				LOCK_SUFFIX;
	}

	@Override
	public String getPriorityPollFromGroupExcludeIdsQuery() {
		return SELECT_COMMON +
				"AND %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID NOT IN (:message_ids) " +
				"ORDER BY MESSAGE_PRIORITY DESC NULLS LAST, CREATED_DATE, MESSAGE_SEQUENCE " +
				"LIMIT 1 " +
				LOCK_SUFFIX;
	}

	@Override
	public String getPriorityPollFromGroupQuery() {
		return SELECT_COMMON +
				"ORDER BY MESSAGE_PRIORITY DESC NULLS LAST, CREATED_DATE, MESSAGE_SEQUENCE " +
				"LIMIT 1 " +
				LOCK_SUFFIX;
	}

	@Override
	public String getPartitionOffsetQuery() {
		return "SELECT %PREFIX%CHANNEL_MESSAGE_PARTITION.PARTITION_SEQUENCE " +
				"FROM %PREFIX%CHANNEL_MESSAGE_PARTITION " +
				"WHERE %PREFIX%CHANNEL_MESSAGE_PARTITION.GROUP_KEY = :group_key " +
					"AND %PREFIX%CHANNEL_MESSAGE_PARTITION.REGION = :region " +
					"AND %PREFIX%CHANNEL_MESSAGE_PARTITION.PARTITION_KEY = :partition_key " +
				LOCK_SUFFIX;
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
