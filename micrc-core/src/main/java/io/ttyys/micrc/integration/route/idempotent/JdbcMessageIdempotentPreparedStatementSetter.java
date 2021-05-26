package io.ttyys.micrc.integration.route.idempotent;

import org.apache.camel.Exchange;
import org.springframework.integration.util.UUIDConverter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

public class JdbcMessageIdempotentPreparedStatementSetter {
    public void setValues(PreparedStatement preparedStatement, Exchange exchange, String groupId, String region)
            throws SQLException {

        String groupKey = Objects.toString(UUIDConverter.getUUID(groupId), null);
        String partitionKey = Objects.toString(UUIDConverter.getUUID(exchange.getIn().getHeader("PARTITION")),
                UUID.randomUUID().toString());

        preparedStatement.setString(1, exchange.getIn().getMessageId());
        preparedStatement.setString(2, partitionKey);
        preparedStatement.setString(3, groupKey);
        preparedStatement.setString(4, region);
        preparedStatement.setLong(5, exchange.getIn().getHeader("CREATED_AT", Long.class));
        preparedStatement.setLong(6, exchange.getIn().getHeader("PRIORITY", Long.class));
        preparedStatement.setLong(7, exchange.getIn().getHeader("SEQUENCE", Long.class));
    }
}
