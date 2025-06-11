package single.cjj.bizfi.service;

import single.cjj.bizfi.entity.BizfiBaseUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 基础用户信息表 服务类
 * </p>
 *
 * @author micor
 * @since 2025-06-04
 */
public interface BizfiBaseUserService extends IService<BizfiBaseUser> {
    BizfiBaseUser getUserByAccount(String account);
}
