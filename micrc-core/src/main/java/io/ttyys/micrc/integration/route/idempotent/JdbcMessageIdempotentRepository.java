package io.ttyys.micrc.integration.route.idempotent;

import io.ttyys.micrc.integration.route.store.PartitionCapableChannelMessageStore;
import org.apache.camel.Exchange;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.service.ServiceSupport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class JdbcMessageIdempotentRepository extends ServiceSupport
        implements IdempotentRepository, SequenceCapableIdempotentRepository, InitializingBean {
    private static final LogAccessor LOGGER = new LogAccessor(JdbcMessageIdempotentRepository.class);

    public static final String DEFAULT_REGION = "DEFAULT";
    public static final String DEFAULT_TABLE_PREFIX = "CAMEL_";

    private JdbcTemplate jdbcTemplate;
    private String processorName;
    private final Map<Query, String> queryCache = new ConcurrentHashMap<>();
    private String tablePrefix = DEFAULT_TABLE_PREFIX;
    private MessageIdempotentQueryProvider messageIdempotentQueryProvider;
    private String region = DEFAULT_REGION;
    private JdbcMessageIdempotentPreparedStatementSetter preparedStatementSetter;
    private boolean sequenceEnabled;

    private enum Query {
        CREATE_MESSAGE,
        CONTAINS_MESSAGE,
        POLL,
        PARTITION,
        DELETE_MESSAGE,
        CLEAR_GROUP_MESSAGES,
        CLEAR_PARTITION_MESSAGES
    }

    public JdbcMessageIdempotentRepository() {
    }

    public JdbcMessageIdempotentRepository(JdbcTemplate jdbcTemplate,
                                           String processorName) {
        this.processorName = processorName;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean add(final String key) {
        throw new UnsupportedOperationException("no supported operation!");
    }

    @Override
    public boolean contains(final String key) {
        throw new UnsupportedOperationException("no supported operation!");
    }

    @Override
    public boolean remove(final String key) {
        throw new UnsupportedOperationException("no supported operation!");
    }

    @Override
    public boolean confirm(String key) {
        throw new UnsupportedOperationException("no supported operation!");
    }

    @Override
    public void clear() {
        this.jdbcTemplate.update(
                getQuery(Query.CLEAR_GROUP_MESSAGES, () -> this.messageIdempotentQueryProvider.getClearGroupMessages()),
                getKey(this.processorName));
    }

    @Override
    public boolean add(Exchange exchange, String key) {
        if (this.isSequenceEnabled()) {
            sequenceProcess(exchange);
        }
        try {
            this.jdbcTemplate.update(getQuery(Query.CREATE_MESSAGE,
                    () -> this.messageIdempotentQueryProvider.getCreateMessageQuery()),
                    ps -> this.preparedStatementSetter.setValues(ps, exchange, processorName, this.region, key));
            return true;
        } catch (DuplicateKeyException e) {
            LOGGER.debug("The Message with id [" + exchange.getIn().getMessageId() + "] duplicate.\n" +
                            "Ignoring...");
            return false;
        }
    }

    @Override
    public boolean contains(Exchange exchange, String key) {
        return this.jdbcTemplate.queryForObject(
                getQuery(Query.CONTAINS_MESSAGE, () -> this.messageIdempotentQueryProvider.getContainsMessageQuery()),
                Integer.class, getKey(this.processorName), key) != null;
    }

    @Override
    public boolean remove(Exchange exchange, String key) {
        return this.jdbcTemplate.update(
                getQuery(Query.DELETE_MESSAGE, () -> this.messageIdempotentQueryProvider.getDeleteMessageQuery()),
                getKey(this.processorName), key) != 0;
    }

    @Override
    public boolean confirm(Exchange exchange, String key) {
        if (this.isSequenceEnabled()) {
            sequenceClear(exchange);
        }
        return true;
    }

    @Override
    public boolean isSequenceEnabled() {
        return this.sequenceEnabled;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.state(this.jdbcTemplate != null, "A DataSource or JdbcTemplate must be provided");
        Assert.notNull(this.messageIdempotentQueryProvider, "A messageIdempotentQueryProvider must be provided.");

        if (this.jdbcTemplate.getFetchSize() != 1 && LOGGER.isWarnEnabled()) {
            LOGGER.warn("The jdbcTemplate's fetch size is not 1. This may cause FIFO issues with Oracle databases.");
        }

        if (this.preparedStatementSetter == null) {
            this.preparedStatementSetter = new JdbcMessageIdempotentPreparedStatementSetter();
        }
        this.jdbcTemplate.afterPropertiesSet();
    }

    protected void sequenceProcess(Exchange exchange) {
        String partitionId = exchange.getIn().getHeader(
                PartitionCapableChannelMessageStore.MESSAGE_HEADER_PARTITION_ID, String.class);
        if (partitionId.equals(PartitionCapableChannelMessageStore.MESSAGE_HEADER_PARTITION_DEFAULT_ID)) {
            return;
        }
        Long offset = exchange.getIn().getHeader(
                PartitionCapableChannelMessageStore.MESSAGE_HEADER_PARTITION_OFFSET, Long.class);
        if (offset == 0) {
            return;
        }
        String groupId = getKey(this.processorName);
        Long lastSequence = this.jdbcTemplate.queryForObject(
                getQuery(Query.PARTITION, () -> this.messageIdempotentQueryProvider.getPartitionPollQuery()),
                Long.class, partitionId, groupId, this.region);
        if (lastSequence == null) {
            return;
        }
        Long sequence = exchange.getIn().getHeader(
                PartitionCapableChannelMessageStore.MESSAGE_HEADER_PARTITION_SEQUENCE, Long.class);
        if (sequence - lastSequence == offset) {
            return;
        }
        throw new IllegalSequencingRuntimeException();
    }

    protected void sequenceClear(Exchange exchange) {
        String partitionId = exchange.getIn().getHeader(
                PartitionCapableChannelMessageStore.MESSAGE_HEADER_PARTITION_ID, String.class);
        String groupId = getKey(this.processorName);
        this.jdbcTemplate.update(getQuery(Query.CLEAR_PARTITION_MESSAGES,
                () -> this.messageIdempotentQueryProvider.getClearPartitionMessagesQuery()), partitionId, groupId, this.region);
    }

    protected String getQuery(Query queryName, Supplier<String> queryProvider) {
        return this.queryCache.computeIfAbsent(queryName,
                k -> StringUtils.replace(queryProvider.get(), "%PREFIX%", this.tablePrefix));
    }

    public void setMessageIdempotentQueryProvider(MessageIdempotentQueryProvider messageIdempotentQueryProvider) {
        Assert.notNull(messageIdempotentQueryProvider,
                "The provided messageIdempotentQueryProvider must not be null.");
        this.messageIdempotentQueryProvider = messageIdempotentQueryProvider;
    }

    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.setFetchSize(1);
        this.jdbcTemplate.setMaxRows(1);
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setProcessorName(String processorName) {
        this.processorName = processorName;
    }

    public void setSequenceEnabled(boolean sequenceEnabled) {
        this.sequenceEnabled = sequenceEnabled;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    private String getKey(Object input) {
        return input == null ? null : UUIDConverter.getUUID(input).toString();
    }

    static class MessageCleaner {

    }
}
