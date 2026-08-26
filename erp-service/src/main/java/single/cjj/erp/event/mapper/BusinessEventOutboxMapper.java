package single.cjj.erp.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import single.cjj.erp.event.entity.BusinessEventOutboxEntity;

@Mapper
public interface BusinessEventOutboxMapper extends BaseMapper<BusinessEventOutboxEntity> {
}
