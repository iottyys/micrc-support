package io.ttyys.micrc.integration.route.idempotent;

import io.ttyys.micrc.integration.route.store.PartitionCapableChannelMessageStore;
import org.apache.camel.Exchange;
import org.springframework.integration.util.UUIDConverter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public class JdbcMessageIdempotentPreparedStatementSetter {
    public void setValues(PreparedStatement preparedStatement, Exchange exchange,
                          String groupId, String region, String key)
            throws SQLException {

        String groupKey = Objects.toString(UUIDConverter.getUUID(groupId), null);

        preparedStatement.setString(1, key);
        preparedStatement.setString(2,
                exchange.getIn().getHeader(
                        PartitionCapableChannelMessageStore.MESSAGE_HEADER_PARTITION_ID,
                        String.class));
        preparedStatement.setString(3, groupKey);
        preparedStatement.setString(4, region);
        preparedStatement.setLong(5,
                exchange.getIn().getHeader(
                        PartitionCapableChannelMessageStore.MESSAGE_HEADER_PARTITION_CREATED_AT,
                        Long.class));
        preparedStatement.setLong(6,
                exchange.getIn().getHeader(
                        PartitionCapableChannelMessageStore.MESSAGE_HEADER_PARTITION_SEQUENCE,
                        Long.class));
    }
}
