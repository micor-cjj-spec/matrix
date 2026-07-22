package single.cjj.botp.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import single.cjj.botp.persistence.entity.BotpExecutionLogEntity;

@Mapper
public interface BotpExecutionLogMapper extends BaseMapper<BotpExecutionLogEntity> {
}
