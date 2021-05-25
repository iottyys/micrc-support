package io.ttyys.micrc.integration.route.idempotent;

public class H2JdbcMessageIdempotentSequencingRepository extends AbstractJdbcMessageIdempotentSequencingRepository {
    private static final String DEFAULT_TABLE_PREFIX = "CAMEL_";

    private boolean createTableIfNotExists = true;
    private String tableName;

    private String tableExistsString = "SELECT 1 FROM CAMEL_MESSAGEPROCESSED WHERE 1 = 0";
    private String createString
            = "CREATE TABLE CAMEL_MESSAGEPROCESSED (processorName VARCHAR(255), messageId VARCHAR(100), createdAt TIMESTAMP)";
    private String queryString = "SELECT COUNT(*) FROM CAMEL_MESSAGEPROCESSED WHERE processorName = ? AND messageId = ?";
    private String insertString = "INSERT INTO CAMEL_MESSAGEPROCESSED (processorName, messageId, createdAt) VALUES (?, ?, ?)";
    private String deleteString = "DELETE FROM CAMEL_MESSAGEPROCESSED WHERE processorName = ? AND messageId = ?";
    private String clearString = "DELETE FROM CAMEL_MESSAGEPROCESSED WHERE processorName = ?";

    @Override
    protected int queryForInt(String key) {
        return 0;
    }

    @Override
    protected int insert(String key) {
        return 0;
    }

    @Override
    protected int delete(String key) {
        return 0;
    }

    @Override
    protected void delete() {

    }
}
