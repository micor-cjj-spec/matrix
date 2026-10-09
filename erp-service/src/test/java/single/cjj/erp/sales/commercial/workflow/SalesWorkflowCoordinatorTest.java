package single.cjj.erp.sales.commercial.workflow;

import org.junit.jupiter.api.Test;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.sales.commercial.workflow.mapper.SalesWorkflowLinkMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalesWorkflowCoordinatorTest {
    private final SalesWorkflowLinkMapper mapper=mock(SalesWorkflowLinkMapper.class);

    @Test
    void createsStableIdempotentQueueRecord() {
        var coordinator=new SalesWorkflowCoordinator(mapper,true,"sales_quote_v1","sales_contract_v1");
        when(mapper.insert(any())).thenReturn(1);
        coordinator.enqueue("T1",3L,"SALES_QUOTE",100L,7L);
        verify(mapper).insert(argThat(link->
          "PENDING".equals(link.getFstatus())
            && "ERP-SALES-T1-SALES_QUOTE-100".equals(link.getFidempotencyKey())
            && "sales_quote_v1".equals(link.getFdefinitionKey())));
    }

    @Test
    void duplicateWorkflowLinkFails() {
        var coordinator=new SalesWorkflowCoordinator(mapper,true,"quote","contract");
        when(mapper.findDocument("T1","SALES_QUOTE",100L)).thenReturn(new SalesWorkflowLinkEntity());
        assertThrows(BizException.class,()->
             coordinator.enqueue("T1",3L,"SALES_QUOTE",100L,7L));
        verify(mapper,never()).insert(any());
    }

    @Test
    void disabledFeatureNeverTouchesDatabase() {
        var coordinator=new SalesWorkflowCoordinator(mapper,false,"","");
        coordinator.enqueue("T1",3L,"SALES_QUOTE",100L,7L);
        verifyNoInteractions(mapper);
    }
}
