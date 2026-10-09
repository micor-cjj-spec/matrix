package single.cjj.botp.relation;

import org.junit.jupiter.api.Test;
import single.cjj.botp.domain.BotpContracts.DocumentRelationEntry;
import single.cjj.botp.domain.BotpContracts.RelationStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotpRelationEntryRepositoryTest {

    @Test
    void shouldPersistRelationEntryIdempotentlyInMemory() {
        InMemoryBotpRelationRepository repository = new InMemoryBotpRelationRepository();
        DocumentRelationEntry entry = new DocumentRelationEntry(
                null, "default", 1L, "11", "21",
                new BigDecimal("60"), null, null, null, RelationStatus.ACTIVE);
        repository.saveEntries("default", 1L, List.of(entry));
        repository.saveEntries("default", 1L, List.of(entry));
        assertEquals(1, repository.findEntries(1L).size());
        assertEquals(new BigDecimal("60"), repository.findEntries(1L).get(0).quantity());
    }
}
