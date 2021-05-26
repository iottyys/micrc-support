package io.ttyys.micrc.integration.route.idempotent;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.service.ServiceSupport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
public class JdbcMessageIdempotentRepository extends ServiceSupport
        implements IdempotentRepository, SequenceCapableIdempotentRepository, InitializingBean {
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
        COUNT_GROUPS,
        GROUP_SIZE,
        DELETE_GROUP,
        POLL,
        POLL_WITH_EXCLUSIONS,
        PRIORITY,
        PRIORITY_WITH_EXCLUSIONS,
        DELETE_MESSAGE
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
        throw new UnsupportedOperationException("no supported yet!");
    }

    @Override
    public boolean contains(final String key) {
        throw new UnsupportedOperationException("no supported yet!");
    }

    @Override
    public boolean remove(final String key) {
        throw new UnsupportedOperationException("no supported yet!");
    }

    @Override
    public boolean confirm(String key) {
        throw new UnsupportedOperationException("no supported yet!");
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean add(Exchange exchange, String key) {
        try {
            this.jdbcTemplate.update(getQuery(Query.CREATE_MESSAGE,
                    () -> this.messageIdempotentQueryProvider.getCreateMessageQuery()),
                    ps -> this.preparedStatementSetter.setValues(ps, exchange, processorName, this.region));
            if (this.sequenceEnabled) {

            }
            return true;
        } catch (DuplicateKeyException e) {
            log.debug("The Message with id [" + exchange.getIn().getMessageId() + "] duplicate.\n" +
                            "Ignoring...");
            return false;
        }
    }

    @Override
    public boolean contains(Exchange exchange, String key) {
        return false;
    }

    @Override
    public boolean remove(Exchange exchange, String key) {
        return false;
    }

    @Override
    public boolean confirm(Exchange exchange, String key) {
        return false;
    }

    @Override
    public boolean isSequenceEnabled() {
        return this.sequenceEnabled;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.state(this.jdbcTemplate != null, "A DataSource or JdbcTemplate must be provided");
        Assert.notNull(this.messageIdempotentQueryProvider, "A messageIdempotentQueryProvider must be provided.");

        if (this.jdbcTemplate.getFetchSize() != 1 && log.isWarnEnabled()) {
            log.warn("The jdbcTemplate's fetch size is not 1. This may cause FIFO issues with Oracle databases.");
        }

        if (this.preparedStatementSetter == null) {
            this.preparedStatementSetter = new JdbcMessageIdempotentPreparedStatementSetter();
        }
        this.jdbcTemplate.afterPropertiesSet();
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
}
