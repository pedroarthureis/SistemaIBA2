package com.iba.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Registra eventos após commit, sem escrever nomes, CPF ou valores no log. */
public final class AuditEvents {
    private static final Logger LOG = LoggerFactory.getLogger("com.iba.audit");
    private AuditEvents() { }

    public static void afterCommit(String action, String resource, Long id) {
        Runnable write = () -> LOG.info("AUDIT action={} resource={} id={}", action, resource, id);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { write.run(); }
            });
        } else {
            write.run();
        }
    }
}
