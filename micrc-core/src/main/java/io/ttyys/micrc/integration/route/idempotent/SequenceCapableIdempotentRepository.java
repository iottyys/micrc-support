package io.ttyys.micrc.integration.route.idempotent;

public interface SequenceCapableIdempotentRepository {
    boolean isSequenceEnabled();
}
