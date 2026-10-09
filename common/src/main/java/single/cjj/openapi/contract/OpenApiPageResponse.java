package single.cjj.openapi.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiPageResponse<T> {

    private long total;
    private int pageNo;
    private int pageSize;
    private List<T> items;

    public static <T> OpenApiPageResponse<T> of(long total, int pageNo, int pageSize, List<T> items) {
        return new OpenApiPageResponse<>(
                total,
                pageNo,
                pageSize,
                items == null ? Collections.emptyList() : items
        );
    }
}
