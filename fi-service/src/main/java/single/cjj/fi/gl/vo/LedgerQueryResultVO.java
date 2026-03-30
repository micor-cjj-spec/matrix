package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerQueryResultVO {
    private List<?> records = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private Map<String, Object> summary = new LinkedHashMap<>();
}
