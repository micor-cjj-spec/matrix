package single.cjj.bizfi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import single.cjj.bizfi.entity.BizfiBaseUser;

import java.util.List;
import java.util.Map;

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

    BizfiBaseUser addUser(BizfiBaseUser user);

    boolean deleteUser(Long fid);

    BizfiBaseUser updateUser(BizfiBaseUser user);

    IPage<BizfiBaseUser> getUserList(int page, int size, Map<String, Object> query);

    BizfiBaseUser getUserById(Long fid);

    boolean deleteBatch(List<Long> fids);
}
