package single.cjj.fi.gl.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.report.entity.BizfiFiReportItem;
import single.cjj.fi.gl.report.mapper.BizfiFiReportItemMapper;
import single.cjj.fi.gl.report.service.BizfiFiReportItemService;

import java.util.List;
import java.util.Map;

@Service
public class BizfiFiReportItemServiceImpl
        extends ServiceImpl<BizfiFiReportItemMapper, BizfiFiReportItem>
        implements BizfiFiReportItemService {

    @Autowired
    private BizfiFiReportItemMapper mapper;

    @Override
    public BizfiFiReportItem add(BizfiFiReportItem item) {
        mapper.insert(item);
        return item;
    }

    @Override
    public BizfiFiReportItem update(BizfiFiReportItem item) {
        mapper.updateById(item);
        return mapper.selectById(item.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiReportItem get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiReportItem> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiReportItem> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.get("ftemplateId") != null) {
                wrapper.eq(BizfiFiReportItem::getFtemplateId, query.get("ftemplateId"));
            }
            if (StringUtils.hasText((String) query.get("fcode"))) {
                wrapper.like(BizfiFiReportItem::getFcode, query.get("fcode"));
            }
            if (StringUtils.hasText((String) query.get("fname"))) {
                wrapper.like(BizfiFiReportItem::getFname, query.get("fname"));
            }
        }
        wrapper.orderByAsc(BizfiFiReportItem::getFsort)
                .orderByAsc(BizfiFiReportItem::getFid);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<BizfiFiReportItem> listByTemplateId(Long templateId) {
        LambdaQueryWrapper<BizfiFiReportItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizfiFiReportItem::getFtemplateId, templateId)
                .orderByAsc(BizfiFiReportItem::getFsort)
                .orderByAsc(BizfiFiReportItem::getFid);
        return mapper.selectList(wrapper);
    }
}

