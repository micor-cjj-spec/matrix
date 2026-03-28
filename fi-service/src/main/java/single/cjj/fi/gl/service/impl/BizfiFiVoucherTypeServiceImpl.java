package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.gl.entity.BizfiFiVoucherType;
import single.cjj.fi.gl.mapper.BizfiFiVoucherTypeMapper;
import single.cjj.fi.gl.service.BizfiFiVoucherTypeService;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * 凭证字服务实现
 */
@Service
public class BizfiFiVoucherTypeServiceImpl
        extends ServiceImpl<BizfiFiVoucherTypeMapper, BizfiFiVoucherType>
        implements BizfiFiVoucherTypeService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    @Autowired
    private BizfiFiVoucherTypeMapper mapper;

    @Override
    public BizfiFiVoucherType add(BizfiFiVoucherType voucherType) {
        normalizeAndValidate(voucherType, null);
        ensureUnique(voucherType.getForg(), voucherType.getFcode(), null);
        LocalDateTime now = LocalDateTime.now();
        voucherType.setFcreatetime(now);
        voucherType.setFupdatetime(now);
        mapper.insert(voucherType);
        return voucherType;
    }

    @Override
    public BizfiFiVoucherType update(BizfiFiVoucherType voucherType) {
        if (voucherType == null || voucherType.getFid() == null) {
            throw new BizException("凭证字ID不能为空");
        }
        BizfiFiVoucherType db = get(voucherType.getFid());
        if (db == null) {
            throw new BizException("凭证字不存在");
        }
        normalizeAndValidate(voucherType, db);
        ensureUnique(voucherType.getForg(), voucherType.getFcode(), voucherType.getFid());
        voucherType.setFcreatetime(db.getFcreatetime());
        voucherType.setFupdatetime(LocalDateTime.now());
        mapper.updateById(voucherType);
        return mapper.selectById(voucherType.getFid());
    }

    @Override
    public boolean delete(Long fid) {
        return mapper.deleteById(fid) > 0;
    }

    @Override
    public BizfiFiVoucherType get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiVoucherType> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiVoucherType> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.get("forg") instanceof Number number) {
                wrapper.eq(BizfiFiVoucherType::getForg, number.longValue());
            }
            if (StringUtils.hasText((String) query.get("fstatus"))) {
                wrapper.eq(BizfiFiVoucherType::getFstatus,
                        query.get("fstatus").toString().trim().toUpperCase(Locale.ROOT));
            }
            if (StringUtils.hasText((String) query.get("fcode"))) {
                wrapper.like(BizfiFiVoucherType::getFcode, query.get("fcode").toString().trim());
            }
            if (StringUtils.hasText((String) query.get("fname"))) {
                wrapper.like(BizfiFiVoucherType::getFname, query.get("fname").toString().trim());
            }
        }
        wrapper.orderByAsc(BizfiFiVoucherType::getForg)
                .orderByAsc(BizfiFiVoucherType::getFsort)
                .orderByAsc(BizfiFiVoucherType::getFcode);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public BizfiFiVoucherType enable(Long fid) {
        return changeStatus(fid, STATUS_ENABLED);
    }

    @Override
    public BizfiFiVoucherType disable(Long fid) {
        return changeStatus(fid, STATUS_DISABLED);
    }

    private BizfiFiVoucherType changeStatus(Long fid, String status) {
        BizfiFiVoucherType db = mustGet(fid);
        db.setFstatus(status);
        db.setFupdatetime(LocalDateTime.now());
        mapper.updateById(db);
        return mapper.selectById(fid);
    }

    private BizfiFiVoucherType mustGet(Long fid) {
        if (fid == null) {
            throw new BizException("凭证字ID不能为空");
        }
        BizfiFiVoucherType db = mapper.selectById(fid);
        if (db == null) {
            throw new BizException("凭证字不存在");
        }
        return db;
    }

    private void ensureUnique(Long forg, String code, Long excludeId) {
        LambdaQueryWrapper<BizfiFiVoucherType> wrapper = new LambdaQueryWrapper<BizfiFiVoucherType>()
                .eq(BizfiFiVoucherType::getForg, forg)
                .eq(BizfiFiVoucherType::getFcode, code)
                .ne(excludeId != null, BizfiFiVoucherType::getFid, excludeId);
        if (mapper.selectCount(wrapper) > 0) {
            throw new BizException("同组织下凭证字编码已存在: " + code);
        }
    }

    private void normalizeAndValidate(BizfiFiVoucherType voucherType, BizfiFiVoucherType existing) {
        if (voucherType == null) {
            throw new BizException("凭证字参数不能为空");
        }
        if (voucherType.getForg() == null) {
            throw new BizException("组织ID不能为空");
        }
        if (!StringUtils.hasText(voucherType.getFcode())) {
            throw new BizException("凭证字编码不能为空");
        }
        if (!StringUtils.hasText(voucherType.getFname())) {
            throw new BizException("凭证字名称不能为空");
        }

        voucherType.setFcode(voucherType.getFcode().trim().toUpperCase(Locale.ROOT));
        voucherType.setFname(voucherType.getFname().trim());
        voucherType.setFprefix(trimToNull(voucherType.getFprefix()));
        voucherType.setFsort(voucherType.getFsort() == null ? 0 : voucherType.getFsort());

        String defaultStatus = existing != null && StringUtils.hasText(existing.getFstatus())
                ? existing.getFstatus() : STATUS_ENABLED;
        String status = StringUtils.hasText(voucherType.getFstatus())
                ? voucherType.getFstatus().trim().toUpperCase(Locale.ROOT) : defaultStatus;
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BizException("凭证字状态仅支持ENABLED/DISABLED");
        }
        voucherType.setFstatus(status);
        voucherType.setFremark(trimToNull(voucherType.getFremark()));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
