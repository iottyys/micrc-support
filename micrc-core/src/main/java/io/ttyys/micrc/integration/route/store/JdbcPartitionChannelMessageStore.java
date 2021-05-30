package io.ttyys.micrc.integration.route.store;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.log.LogAccessor;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.integration.jdbc.store.channel.ChannelMessageStorePreparedStatementSetter;
import org.springframework.integration.jdbc.store.channel.MessageRowMapper;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupFactory;
import org.springframework.integration.store.SimpleMessageGroupFactory;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class JdbcPartitionChannelMessageStore implements PartitionCapableChannelMessageStore, InitializingBean {

    private static final LogAccessor LOGGER = new LogAccessor(JdbcPartitionChannelMessageStore.class);

    public static final String DEFAULT_REGION = "DEFAULT";
    public static final String DEFAULT_TABLE_PREFIX = "INT_";

    private enum Query {
        CREATE_MESSAGE,
        COUNT_GROUPS,
        GROUP_SIZE,
        DELETE_GROUP,
        POLL,
        POLL_WITH_EXCLUSIONS,
        PRIORITY,
        PRIORITY_WITH_EXCLUSIONS,
        DELETE_MESSAGE,
        POLL_PARTITION_OFFSET,
        UPDATE_PARTITION_OFFSET,
        INSERT_PARTITION_OFFSET
    }

    private final Set<String> idCache = new HashSet<>();

    private final ReadWriteLock idCacheLock = new ReentrantReadWriteLock();

    private final Lock idCacheReadLock = this.idCacheLock.readLock();

    private final Lock idCacheWriteLock = this.idCacheLock.writeLock();

    private PartitionChannelMessageStoreQueryProvider channelMessageStoreQueryProvider;

    private String region = DEFAULT_REGION;

    private String tablePrefix = DEFAULT_TABLE_PREFIX;

    private JdbcTemplate jdbcTemplate;

    private AllowListDeserializingConverter deserializer;

    private SerializingConverter serializer;

    private LobHandler lobHandler = new DefaultLobHandler();

    private MessageRowMapper messageRowMapper;

    private ChannelMessageStorePreparedStatementSetter preparedStatementSetter;

    private final Map<Query, String> queryCache = new ConcurrentHashMap<>();

    private MessageGroupFactory messageGroupFactory = new SimpleMessageGroupFactory();

    private boolean usingIdCache = false;

    private boolean priorityEnabled;

    private boolean partitionEnabled;

    public JdbcPartitionChannelMessageStore() {
        this.deserializer = new AllowListDeserializingConverter();
        this.serializer = new SerializingConverter();
    }

    public JdbcPartitionChannelMessageStore(DataSource dataSource) {
        this();
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.setFetchSize(1);
        this.jdbcTemplate.setMaxRows(1);
    }

    @Override
    public MessageGroup addMessageToGroup(Object groupId, final Message<?> message) {
        try {
            this.jdbcTemplate.update(getQuery(Query.CREATE_MESSAGE,
                    () -> this.channelMessageStoreQueryProvider.getCreateMessageQuery()),
                    ps -> this.preparedStatementSetter.setValues(ps, message, groupId, this.region,
                            this.priorityEnabled));
        }
        catch (@SuppressWarnings("unused") DuplicateKeyException e) {
            LOGGER.debug(() ->
                    "The Message with id [" + getKey(message.getHeaders().getId()) + "] already exists.\n" +
                            "Ignoring INSERT...");
        }
        return getMessageGroup(groupId);
    }

    @Override
    public Message<?> pollMessageFromGroup(Object groupId) {
        String key = getKey(groupId);
        Message<?> polledMessage = doPollForMessage(key);
        if (polledMessage != null && !doRemoveMessageFromGroup(groupId, polledMessage)) {
            return null;
        }
        return polledMessage;
    }

    @Override
    public MessageGroup getMessageGroup(Object groupId) {
        return getMessageGroupFactory().create(groupId);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    @ManagedAttribute
    public int messageGroupSize(Object groupId) {
        final String key = getKey(groupId);
        return this.jdbcTemplate.queryForObject(
                getQuery(Query.GROUP_SIZE,
                        () -> this.channelMessageStoreQueryProvider.getCountAllMessagesInGroupQuery()),
                Integer.class, key, this.region);
    }

    @Override
    public void removeMessageGroup(Object groupId) {
        this.jdbcTemplate.update(
                getQuery(Query.DELETE_GROUP,
                        () -> this.channelMessageStoreQueryProvider.getDeleteMessageGroupQuery()),
                getKey(groupId), this.region);
    }

    @Override
    public boolean isPriorityEnabled() {
        return this.priorityEnabled;
    }

    @Override
    public boolean isPartitionEnabled() {
        return this.partitionEnabled;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.state(this.jdbcTemplate != null, "A DataSource or JdbcTemplate must be provided");
        Assert.notNull(this.channelMessageStoreQueryProvider, "A channelMessageStoreQueryProvider must be provided.");

        if (this.messageRowMapper == null) {
            this.messageRowMapper = new MessageRowMapper(this.deserializer, this.lobHandler);
        }

        if (this.jdbcTemplate.getFetchSize() != 1 && LOGGER.isWarnEnabled()) {
            LOGGER.warn("The jdbcTemplate's fetch size is not 1. This may cause FIFO issues with Oracle databases.");
        }

        if (this.preparedStatementSetter == null) {
            this.preparedStatementSetter = new ChannelMessageStorePreparedStatementSetter(this.serializer,
                    this.lobHandler);
        }
        this.jdbcTemplate.afterPropertiesSet();
    }

    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.setFetchSize(1);
        this.jdbcTemplate.setMaxRows(1);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setDeserializer(Deserializer<? extends Message<?>> deserializer) {
        this.deserializer = new AllowListDeserializingConverter((Deserializer) deserializer);
    }

    public void addAllowedPatterns(String... patterns) {
        this.deserializer.addAllowedPatterns(patterns);
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate, "The provided jdbcTemplate must not be null.");
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setLobHandler(LobHandler lobHandler) {
        Assert.notNull(lobHandler, "The provided LobHandler must not be null.");
        this.lobHandler = lobHandler;
    }

    public void setMessageRowMapper(MessageRowMapper messageRowMapper) {
        Assert.notNull(messageRowMapper, "The provided MessageRowMapper must not be null.");
        this.messageRowMapper = messageRowMapper;
    }

    public void setPreparedStatementSetter(ChannelMessageStorePreparedStatementSetter preparedStatementSetter) {
        Assert.notNull(preparedStatementSetter,
                "The provided ChannelMessageStorePreparedStatementSetter must not be null.");
        this.preparedStatementSetter = preparedStatementSetter;
    }

    public void setChannelMessageStoreQueryProvider(PartitionChannelMessageStoreQueryProvider channelMessageStoreQueryProvider) {
        Assert.notNull(channelMessageStoreQueryProvider,
                "The provided channelMessageStoreQueryProvider must not be null.");
        this.channelMessageStoreQueryProvider = channelMessageStoreQueryProvider;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @SuppressWarnings("unchecked")
    public void setSerializer(Serializer<? super Message<?>> serializer) {
        Assert.notNull(serializer, "The provided serializer must not be null.");
        this.serializer = new SerializingConverter((Serializer<Object>) serializer);
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public void setUsingIdCache(boolean usingIdCache) {
        this.usingIdCache = usingIdCache;
    }

    public void setPriorityEnabled(boolean priorityEnabled) {
        this.priorityEnabled = priorityEnabled;
    }

    public void setPartitionEnabled(boolean partitionEnabled) {
        this.partitionEnabled = partitionEnabled;
    }

    public void setMessageGroupFactory(MessageGroupFactory messageGroupFactory) {
        Assert.notNull(messageGroupFactory, "'messageGroupFactory' must not be null");
        this.messageGroupFactory = messageGroupFactory;
    }

    @SuppressWarnings("ConstantConditions")
    @ManagedAttribute
    public int getMessageGroupCount() {
        return this.jdbcTemplate.queryForObject(
                getQuery(Query.COUNT_GROUPS,
                        () -> "SELECT COUNT(DISTINCT GROUP_KEY) from %PREFIX%CHANNEL_MESSAGE where REGION = ?"),
                Integer.class, this.region);
    }

    public void removeFromIdCache(String messageId) {
        LOGGER.debug(() -> "Removing Message Id: " + messageId);
        this.idCacheWriteLock.lock();
        try {
            this.idCache.remove(messageId);
        }
        finally {
            this.idCacheWriteLock.unlock();
        }
    }

    @ManagedMetric
    public int getSizeOfIdCache() {
        return this.idCache.size();
    }

    protected MessageGroupFactory getMessageGroupFactory() {
        return this.messageGroupFactory;
    }

    protected String getQuery(Query queryName, Supplier<String> queryProvider) {
        return this.queryCache.computeIfAbsent(queryName,
                k -> StringUtils.replace(queryProvider.get(), "%PREFIX%", this.tablePrefix));
    }

    protected Message<?> doPollForMessage(String groupIdKey) {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(this.jdbcTemplate);
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        parameters.addValue("region", this.region);
        parameters.addValue("group_key", groupIdKey);

        String query;

        final List<Message<?>> messages;

        this.idCacheReadLock.lock();
        try {
            if (this.usingIdCache && !this.idCache.isEmpty()) {
                if (this.priorityEnabled) {
                    query = getQuery(Query.PRIORITY_WITH_EXCLUSIONS,
                            () -> this.channelMessageStoreQueryProvider.getPriorityPollFromGroupExcludeIdsQuery());
                }
                else {
                    query = getQuery(Query.POLL_WITH_EXCLUSIONS,
                            () -> this.channelMessageStoreQueryProvider.getPollFromGroupExcludeIdsQuery());
                }
                parameters.addValue("message_ids", this.idCache);
            }
            else {
                if (this.priorityEnabled) {
                    query = getQuery(Query.PRIORITY,
                            () -> this.channelMessageStoreQueryProvider.getPriorityPollFromGroupQuery());
                }
                else {
                    query = getQuery(Query.POLL, () -> this.channelMessageStoreQueryProvider.getPollFromGroupQuery());
                }
            }
            messages = namedParameterJdbcTemplate.query(query, parameters, this.messageRowMapper);
            if (this.isPartitionEnabled()) {
                partitionProcess(messages, namedParameterJdbcTemplate, groupIdKey);
            }
        }
        finally {
            this.idCacheReadLock.unlock();
        }


        Assert.state(messages.size() < 2,
                () -> "The query must return zero or 1 row; got " + messages.size() + " rows");
        if (messages.size() > 0) {

            final Message<?> message = messages.get(0);
            UUID id = message.getHeaders().getId();
            Assert.state(id != null, "Messages must have an id header to be stored");
            final String messageId = id.toString();

            if (this.usingIdCache) {
                this.idCacheWriteLock.lock();
                try {
                    boolean added = this.idCache.add(messageId);
                    LOGGER.debug(() -> String.format("Polled message with id '%s' added: '%s'.", messageId, added));
                }
                finally {
                    this.idCacheWriteLock.unlock();
                }
            }

            return message;
        }
        return null;
    }

    private void partitionProcess(List<Message<?>> messages, NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                  String groupIdKey) {
        Assert.state(messages.size() < 2,
                () -> "The query must return zero or 1 row; got " + messages.size() + " rows");
        if (messages.size() > 0) {
            final Message<?> message = messages.get(0);
            String partitionKey = message.getHeaders().get(MESSAGE_HEADER_PARTITION_KEY, String.class);
            if (!StringUtils.hasText(partitionKey)) {
                message.getHeaders().put(MESSAGE_HEADER_PARTITION_ID, MESSAGE_HEADER_PARTITION_DEFAULT_ID);
                return;
            }
            String partitionIdKey = getKey(partitionKey);
            message.getHeaders().put(MESSAGE_HEADER_PARTITION_ID, partitionIdKey);
            Long sequence = message.getHeaders().get(MESSAGE_HEADER_PARTITION_SEQUENCE, Long.class);
            Assert.state(sequence != null,
                    "Message that support partition must has header: "
                            + MESSAGE_HEADER_PARTITION_SEQUENCE + " with non-null value. ");
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("region", this.region);
            parameters.addValue("group_key", groupIdKey);
            parameters.addValue("partition_key", partitionKey);
            Long preSequence = namedParameterJdbcTemplate.queryForObject(getQuery(Query.POLL_PARTITION_OFFSET,
                    () -> this.channelMessageStoreQueryProvider.getPartitionOffsetQuery()), parameters, Long.class);
            parameters.addValue("partition_sequence", sequence);
            if (preSequence == null) {
                namedParameterJdbcTemplate.update(getQuery(Query.INSERT_PARTITION_OFFSET,
                        () -> this.channelMessageStoreQueryProvider.getCreatePartitionOffsetQuery()), parameters);
                message.getHeaders().put(MESSAGE_HEADER_PARTITION_OFFSET, 0);
                return;
            }
            namedParameterJdbcTemplate.update(getQuery(Query.UPDATE_PARTITION_OFFSET,
                    () -> this.channelMessageStoreQueryProvider.getUpdatePartitionOffsetQuery()), parameters);
            message.getHeaders().put(MESSAGE_HEADER_PARTITION_OFFSET, sequence - preSequence);
        }
    }

    private boolean doRemoveMessageFromGroup(Object groupId, Message<?> messageToRemove) {
        UUID id = messageToRemove.getHeaders().getId();
        int updated = this.jdbcTemplate.update(
                getQuery(Query.DELETE_MESSAGE, () -> this.channelMessageStoreQueryProvider.getDeleteMessageQuery()),
                new Object[]{ getKey(id), getKey(groupId), this.region },
                new int[]{ Types.VARCHAR, Types.VARCHAR, Types.VARCHAR });

        boolean result = updated != 0;
        if (result) {
            LOGGER.debug(() -> String.format("Message with id '%s' was deleted.", id));
        }
        else {
            LOGGER.warn(() -> String.format("Message with id '%s' was not deleted.", id));
        }
        return result;
    }

    private String getKey(Object input) {
        return input == null ? null : UUIDConverter.getUUID(input).toString();
    }
}
