package single.cjj.botp.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import single.cjj.botp.persistence.entity.BotpWritebackTaskEntity;

@Mapper
public interface BotpWritebackTaskMapper extends BaseMapper<BotpWritebackTaskEntity> {
}
