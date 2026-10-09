package single.cjj.bizfi.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeBaseAcl;

import java.util.List;
import java.util.Set;

@Mapper
public interface BizfiAiKnowledgeBaseAclMapper extends BaseMapper<BizfiAiKnowledgeBaseAcl> {

    @Select("""
            <script>
            SELECT fid, fkbid, fsubjecttype, fsubjectid, fpermission,
                   fcreatedby, fcreatetime, fmodifytime
              FROM bizfi_ai_knowledge_base_acl
             WHERE
             <if test="kbId != null and kbId != ''">
                   fkbid = #{kbId}
               AND
             </if>
                   (
                       (fsubjecttype = 'USER' AND fsubjectid = #{userId})
                       <if test="organizationIds != null and organizationIds.size > 0">
                       OR (fsubjecttype = 'ORGANIZATION' AND fsubjectid IN
                           <foreach collection="organizationIds" item="organizationId" open="(" separator="," close=")">
                               #{organizationId}
                           </foreach>
                       )
                       </if>
                       <if test="authorities != null and authorities.size > 0">
                       OR (fsubjecttype = 'AUTHORITY' AND fsubjectid IN
                           <foreach collection="authorities" item="authority" open="(" separator="," close=")">
                               #{authority}
                           </foreach>
                       )
                       </if>
                   )
            </script>
            """)
    List<BizfiAiKnowledgeBaseAcl> selectMatching(
            @Param("userId") String userId,
            @Param("organizationIds") Set<String> organizationIds,
            @Param("authorities") Set<String> authorities,
            @Param("kbId") String kbId
    );
}
