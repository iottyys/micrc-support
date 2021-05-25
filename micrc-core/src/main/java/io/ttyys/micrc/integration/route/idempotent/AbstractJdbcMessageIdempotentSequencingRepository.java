package io.ttyys.micrc.integration.route.idempotent;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.service.ServiceSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public abstract class AbstractJdbcMessageIdempotentSequencingRepository
        extends ServiceSupport implements IdempotentRepository {
    protected JdbcTemplate jdbcTemplate;
    protected String processorName;
    protected TransactionTemplate transactionTemplate;

    public AbstractJdbcMessageIdempotentSequencingRepository() {
    }

    public AbstractJdbcMessageIdempotentSequencingRepository(JdbcTemplate jdbcTemplate,
                                                             PlatformTransactionManager transactionManager,
                                                             String processorName) {
        this.processorName = processorName;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = createTransactionTemplate(transactionManager);
    }

    protected abstract int queryForInt(String key);
    protected abstract int insert(String key);
    protected abstract int delete(String key);
    protected abstract void delete();

    @Override
    public boolean add(final String key) {
        Boolean rc = transactionTemplate.execute(status -> {
            int count = queryForInt(key);
            if (count == 0) {
                insert(key);
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        });
        assert rc != null;
        return rc;
    }

    @Override
    public boolean contains(final String key) {
        Boolean rc = transactionTemplate.execute(status -> {
            int count = queryForInt(key);
            if (count == 0) {
                return Boolean.FALSE;
            } else {
                return Boolean.TRUE;
            }
        });
        assert rc != null;
        return rc;
    }

    @Override
    public boolean remove(final String key) {
        Boolean rc = transactionTemplate.execute(status -> {
            int updateCount = delete(key);
            if (updateCount == 0) {
                return Boolean.FALSE;
            } else {
                return Boolean.TRUE;
            }
        });
        assert rc != null;
        return rc;
    }

    @Override
    public boolean confirm(String key) {
        return true;
    }

    @Override
    public void clear() {
        transactionTemplate.execute(status -> {
            delete();
            return Boolean.TRUE;
        });
    }

    protected TransactionTemplate createTransactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setTransactionManager(transactionManager);
        transactionTemplate.setReadOnly(false);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.setTimeout(transactionTimeout());
        transactionTemplate.setIsolationLevel(transactionIsolationLevel());
        return transactionTemplate;
    }

    protected int transactionTimeout() {
        return TransactionDefinition.TIMEOUT_DEFAULT;
    }

    protected int transactionIsolationLevel() {
        return TransactionDefinition.ISOLATION_DEFAULT;
    }
}
