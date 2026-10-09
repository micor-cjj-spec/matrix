package single.cjj.bizfi.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
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

    @Insert("""
            INSERT INTO matrix_auth_sales_role_grant
              (fid, ftenant_id, forg_id, fuser_id, frole_code, fstatus,
               fcreate_by, fcreate_time, fmodify_by, fmodify_time, fdelete_flag, fversion)
            VALUES (#{id}, #{tenantId}, #{orgId}, #{userId}, #{role}, 'ACTIVE',
                    #{actorId}, NOW(), #{actorId}, NOW(), 0, 0)
            ON DUPLICATE KEY UPDATE fstatus='ACTIVE', fmodify_by=#{actorId},
                                    fmodify_time=NOW(), fversion=fversion+1
            """)
    int grant(@Param("id") Long id, @Param("tenantId") String tenantId,
              @Param("orgId") Long orgId, @Param("userId") Long userId,
              @Param("role") String role, @Param("actorId") Long actorId);

    @Update("""
            UPDATE matrix_auth_sales_role_grant
               SET fstatus='REVOKED', fmodify_by=#{actorId}, fmodify_time=NOW(),
                   fversion=fversion+1
             WHERE ftenant_id=#{tenantId} AND forg_id=#{orgId}
               AND fuser_id=#{userId} AND frole_code=#{role}
               AND fstatus='ACTIVE' AND fdelete_flag=0
            """)
    int revoke(@Param("tenantId") String tenantId, @Param("orgId") Long orgId,
               @Param("userId") Long userId, @Param("role") String role,
               @Param("actorId") Long actorId);

    @Insert("""
            INSERT INTO matrix_auth_sales_role_grant_audit
               (fid, ftenant_id, forg_id, fuser_id, frole_code, faction,
                foperator_id, fcreate_time)
            VALUES (#{id}, #{tenantId}, #{orgId}, #{userId}, #{role},
                    #{action}, #{actorId}, NOW())
            """)
    int appendAudit(@Param("id") Long id, @Param("tenantId") String tenantId,
                    @Param("orgId") Long orgId, @Param("userId") Long userId,
                    @Param("role") String role, @Param("action") String action,
                    @Param("actorId") Long actorId);
}
