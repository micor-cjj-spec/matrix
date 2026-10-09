package single.cjj.fi.ar.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;

@Mapper
public interface BotpArapIntegrationMapper {

    @Select("SELECT * FROM bizfi_fi_arap_doc WHERE fid = #{fid} FOR UPDATE")
    BizfiFiArapDoc selectByIdForUpdate(@Param("fid") Long fid);
}
