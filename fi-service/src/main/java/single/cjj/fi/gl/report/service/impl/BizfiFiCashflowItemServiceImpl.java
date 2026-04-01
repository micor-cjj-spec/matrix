package single.cjj.fi.gl.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.report.entity.BizfiFiCashflowItem;
import single.cjj.fi.gl.report.mapper.BizfiFiCashflowItemMapper;
import single.cjj.fi.gl.report.service.BizfiFiCashflowItemService;

import java.util.Map;

@Service
public class BizfiFiCashflowItemServiceImpl
        extends ServiceImpl<BizfiFiCashflowItemMapper, BizfiFiCashflowItem>
        implements BizfiFiCashflowItemService {

    @Autowired
    private BizfiFiCashflowItemMapper mapper;

    @Override
    public BizfiFiCashflowItem add(BizfiFiCashflowItem item) {
        mapper.insert(item);
        return item;
    }

    @Override
    public BizfiFiCashflowItem update(BizfiFiCashflowItem item) {
        mapper.updateById(item);
        return mapper.selectById(item.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiCashflowItem get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiCashflowItem> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiCashflowItem> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText((String) query.get("fcode"))) {
                wrapper.like(BizfiFiCashflowItem::getFcode, query.get("fcode"));
            }
            if (StringUtils.hasText((String) query.get("fname"))) {
                wrapper.like(BizfiFiCashflowItem::getFname, query.get("fname"));
            }
            if (StringUtils.hasText((String) query.get("fcategory"))) {
                wrapper.eq(BizfiFiCashflowItem::getFcategory, query.get("fcategory"));
            }
            if (StringUtils.hasText((String) query.get("fstatus"))) {
                wrapper.eq(BizfiFiCashflowItem::getFstatus, query.get("fstatus"));
            }
        }
        wrapper.orderByAsc(BizfiFiCashflowItem::getFsort)
                .orderByAsc(BizfiFiCashflowItem::getFid);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }
}
