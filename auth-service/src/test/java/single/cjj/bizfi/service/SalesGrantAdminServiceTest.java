package single.cjj.bizfi.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import single.cjj.bizfi.mapper.SalesRoleGrantMapper;
import single.cjj.bizfi.security.SalesAclRevision;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalesGrantAdminServiceTest {
    SalesRoleGrantMapper grants = mock(SalesRoleGrantMapper.class);
    SalesAclRevision revisions = mock(SalesAclRevision.class);
    SalesGrantAdminService service = new SalesGrantAdminService(grants, revisions);

    @BeforeEach
    void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void clearTransaction() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private void complete(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(callback -> callback.afterCompletion(status));
    }

    @Test
    void grantMustLockRevisionBeforeDbChangeAndReleaseAfterCommit() {
        service.change("T1", 3L, 77L, "SALES_EDITOR", true, 99L);
        var ordered = inOrder(revisions, grants);
        ordered.verify(revisions).beginChange("T1", 3L, 77L);
        ordered.verify(grants).grant(anyLong(), eq("T1"), eq(3L), eq(77L),
                eq("SALES_EDITOR"), eq(99L));
        ordered.verify(grants).appendAudit(anyLong(), eq("T1"), eq(3L), eq(77L),
                eq("SALES_EDITOR"), eq("GRANT"), eq(99L));
        verify(revisions, never()).finishChange(anyString(), anyLong(), anyLong());
        complete(TransactionSynchronization.STATUS_COMMITTED);
        verify(revisions).finishChange("T1", 3L, 77L);
    }

    @Test
    void revokeReleasesRevisionEvenOnRollbackToRejectPreChangeTokens() {
        service.change("T1", 3L, 77L, "SALES_APPROVER", false, 99L);
        verify(grants).revoke("T1", 3L, 77L, "SALES_APPROVER", 99L);
        verify(grants).appendAudit(anyLong(), eq("T1"), eq(3L), eq(77L),
                eq("SALES_APPROVER"), eq("REVOKE"), eq(99L));
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(revisions).finishChange("T1", 3L, 77L);
    }

    @Test
    void redisLockFailureMustPreventAllWritesAndAudit() {
        doThrow(new IllegalStateException("locked")).when(revisions)
                .beginChange("T1", 3L, 77L);
        assertThrows(IllegalStateException.class,
                () -> service.change("T1", 3L, 77L, "SALES_EDITOR", true, 99L));
        verifyNoInteractions(grants);
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test
    void forgedPrivilegeCannotBeInserted() {
        assertThrows(IllegalArgumentException.class,
                () -> service.change("T1", 3L, 77L, "ROLE_ROOT", true, 99L));
        verifyNoInteractions(grants, revisions);
    }

    @Test
    void mutationCannotRunWithoutTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        assertThrows(IllegalStateException.class,
                () -> service.change("T1", 3L, 77L, "SALES_EDITOR", true, 99L));
        verifyNoInteractions(grants, revisions);
    }
}

