package single.cjj.bizfi.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * Read-only role lookup. Role rows are provisioned by trusted identity administrators,
 * never by login clients or user-submitted JWT claims.
 */
@Mapper
public interface SalesRoleGrantMapper {
    @Select("""
            SELECT frole_code
            FROM matrix_auth_sales_role_grant
            WHERE fuser_id = #{userId}
              AND ftenant_id = #{tenantId}
              AND forg_id = #{orgId}
              AND fstatus = 'ACTIVE'
              AND fdelete_flag = 0
            """)
    List<String> activeRoles(@Param("userId") Long userId,
                             @Param("tenantId") String tenantId,
                             @Param("orgId") Long orgId);
}

