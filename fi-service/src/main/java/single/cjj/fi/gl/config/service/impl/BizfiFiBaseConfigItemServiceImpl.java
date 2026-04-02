package single.cjj.fi.gl.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.config.entity.BizfiFiBaseConfigItem;
import single.cjj.fi.gl.config.mapper.BizfiFiBaseConfigItemMapper;
import single.cjj.fi.gl.config.service.BizfiFiBaseConfigItemService;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
public class BizfiFiBaseConfigItemServiceImpl extends ServiceImpl<BizfiFiBaseConfigItemMapper, BizfiFiBaseConfigItem>
        implements BizfiFiBaseConfigItemService {

    @Override
    public BizfiFiBaseConfigItem add(BizfiFiBaseConfigItem item) {
        normalize(item);
        LocalDateTime now = LocalDateTime.now();
        item.setFcreatetime(now);
        item.setFupdatetime(now);
        baseMapper.insert(item);
        return item;
    }

    @Override
    public BizfiFiBaseConfigItem update(BizfiFiBaseConfigItem item) {
        normalize(item);
        item.setFupdatetime(LocalDateTime.now());
        baseMapper.updateById(item);
        return baseMapper.selectById(item.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return baseMapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiBaseConfigItem get(Long fid) {
        return baseMapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiBaseConfigItem> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiBaseConfigItem> wrapper = new LambdaQueryWrapper<>();
        if (query.get("forg") instanceof Number number) {
            wrapper.eq(BizfiFiBaseConfigItem::getForg, number.longValue());
        }
        if (StringUtils.hasText((String) query.get("fcategory"))) {
            wrapper.eq(BizfiFiBaseConfigItem::getFcategory, query.get("fcategory").toString().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText((String) query.get("fstatus"))) {
            wrapper.eq(BizfiFiBaseConfigItem::getFstatus, query.get("fstatus").toString().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText((String) query.get("fcode"))) {
            wrapper.like(BizfiFiBaseConfigItem::getFcode, query.get("fcode").toString().trim());
        }
        if (StringUtils.hasText((String) query.get("fname"))) {
            wrapper.like(BizfiFiBaseConfigItem::getFname, query.get("fname").toString().trim());
        }
        wrapper.orderByAsc(BizfiFiBaseConfigItem::getFcategory)
                .orderByAsc(BizfiFiBaseConfigItem::getFsort)
                .orderByAsc(BizfiFiBaseConfigItem::getFcode);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private void normalize(BizfiFiBaseConfigItem item) {
        if (item == null) {
            return;
        }
        if (StringUtils.hasText(item.getFcategory())) {
            item.setFcategory(item.getFcategory().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(item.getFcode())) {
            item.setFcode(item.getFcode().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(item.getFname())) {
            item.setFname(item.getFname().trim());
        }
        if (StringUtils.hasText(item.getFvalue())) {
            item.setFvalue(item.getFvalue().trim());
        }
        if (StringUtils.hasText(item.getFremark())) {
            item.setFremark(item.getFremark().trim());
        }
        item.setFsort(item.getFsort() == null ? 0 : item.getFsort());
        item.setFstatus(StringUtils.hasText(item.getFstatus()) ? item.getFstatus().trim().toUpperCase(Locale.ROOT) : "ENABLED");
    }
}
