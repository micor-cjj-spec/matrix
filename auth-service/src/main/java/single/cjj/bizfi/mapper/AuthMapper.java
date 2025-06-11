package single.cjj.bizfi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import single.cjj.bizfi.entity.BizfiBaseUser;

/**
 * <p>
 * 基础用户信息表 Mapper 接口
 * </p>
 *
 * @author micor
 * @since 2025-06-04
 */
@Mapper
public interface AuthMapper extends BaseMapper<BizfiBaseUser> {



}
