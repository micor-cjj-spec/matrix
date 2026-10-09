package single.cjj.botp.rule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import single.cjj.botp.domain.BotpContracts.FieldMapping;
import single.cjj.botp.domain.BotpContracts.WritebackMapping;

import java.util.List;

public record RuleSaveRequest(
        @NotBlank String ruleCode,
        @NotBlank String ruleName,
        @NotBlank String sourceSystemCode,
        @NotBlank String sourceDocumentType,
        @NotBlank String targetSystemCode,
        @NotBlank String targetDocumentType,
        List<@Valid FieldMapping> headerMappings,
        List<@Valid FieldMapping> entryMappings,
        List<@Valid WritebackMapping> writebackMappings
) {
    public RuleSaveRequest {
        headerMappings = headerMappings == null ? List.of() : List.copyOf(headerMappings);
        entryMappings = entryMappings == null ? List.of() : List.copyOf(entryMappings);
        writebackMappings = writebackMappings == null ? List.of() : List.copyOf(writebackMappings);
    }
}
