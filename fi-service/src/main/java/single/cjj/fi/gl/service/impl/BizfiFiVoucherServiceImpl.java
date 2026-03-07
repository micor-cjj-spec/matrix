package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.service.BizfiFiVoucherService;

import java.util.Map;

/**
 * 财务凭证服务实现
 */
@Service
public class BizfiFiVoucherServiceImpl extends ServiceImpl<BizfiFiVoucherMapper, BizfiFiVoucher>
        implements BizfiFiVoucherService {

    @Autowired
    private BizfiFiVoucherMapper mapper;

    @Override
    public BizfiFiVoucher add(BizfiFiVoucher voucher) {
        mapper.insert(voucher);
        return voucher;
    }

    @Override
    public BizfiFiVoucher update(BizfiFiVoucher voucher) {
        mapper.updateById(voucher);
        return mapper.selectById(voucher.getFid());
    }

    @Override
    public BizfiFiVoucher get(Long fid) {
        return mapper.selectById(fid);
    }

    @Override
    public IPage<BizfiFiVoucher> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiVoucher> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText((String) query.get("number"))) {
                wrapper.like(BizfiFiVoucher::getFnumber, query.get("number"));
            }
            if (StringUtils.hasText((String) query.get("summary"))) {
                wrapper.like(BizfiFiVoucher::getFsummary, query.get("summary"));
            }
        }
        Page<BizfiFiVoucher> pageObj = new Page<>(page, size);
        return mapper.selectPage(pageObj, wrapper);
    }
}
