package single.cjj.share.service;

import org.springframework.stereotype.Service;
import single.cjj.share.model.ModuleOption;
import single.cjj.share.model.Priority;
import single.cjj.share.model.PriorityOption;
import single.cjj.share.model.StatusDefinition;
import single.cjj.share.model.SlaDefinition;
import single.cjj.share.model.UserSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ReferenceService {
    private final List<PriorityOption> priorities;
    private final List<StatusDefinition> statuses;
    private final List<ModuleOption> modules;
    private final List<UserSummary> users;

    public ReferenceService() {
        this.priorities = initPriorities();
        this.statuses = initStatuses();
        this.modules = initModules();
        this.users = initUsers();
    }

    private List<PriorityOption> initPriorities() {
        List<PriorityOption> list = new ArrayList<>();
        for (Priority priority : Priority.values()) {
            list.add(new PriorityOption(priority.getValue(), priority.getValue(), priority.getColor(), priority.getDefaultSla()));
        }
        return Collections.unmodifiableList(list);
    }

    private List<StatusDefinition> initStatuses() {
        List<StatusDefinition> list = new ArrayList<>();
        list.add(new StatusDefinition("待分诊", List.of("待办", "搁置", "重复关闭"), new SlaDefinition(4, List.of("SLA_BREACH"))));
        list.add(new StatusDefinition("待办", List.of("进行中", "搁置", "阻塞"), new SlaDefinition(24, null)));
        list.add(new StatusDefinition("进行中", List.of("待验收", "阻塞", "搁置"), new SlaDefinition(72, null)));
        list.add(new StatusDefinition("待验收", List.of("已完成", "进行中"), new SlaDefinition(16, null)));
        list.add(new StatusDefinition("已完成", List.of(), null));
        list.add(new StatusDefinition("搁置", List.of("待办"), new SlaDefinition(null, null)));
        list.add(new StatusDefinition("阻塞", List.of("进行中"), new SlaDefinition(null, null)));
        return Collections.unmodifiableList(list);
    }

    private List<ModuleOption> initModules() {
        return List.of(
                new ModuleOption("payment", "支付"),
                new ModuleOption("account", "账户"),
                new ModuleOption("risk", "风控"),
                new ModuleOption("finance", "财务"),
                new ModuleOption("ops", "运营")
        );
    }

    private List<UserSummary> initUsers() {
        return List.of(
                new UserSummary("u_001", "李雷", "lilei@corp.com", null),
                new UserSummary("u_002", "韩梅", "hanmei@corp.com", null),
                new UserSummary("u_003", "张三", "zhangsan@corp.com", null),
                new UserSummary("u_004", "李四", "lisi@corp.com", null),
                new UserSummary("u_005", "王五", "wangwu@corp.com", null)
        );
    }

    public List<PriorityOption> getPriorities() {
        return priorities;
    }

    public List<StatusDefinition> getStatuses(String from) {
        if (from == null || from.isBlank()) {
            return statuses;
        }
        return statuses.stream()
                .filter(status -> status.getStatus().equalsIgnoreCase(from))
                .collect(Collectors.toList());
    }

    public List<ModuleOption> getModules(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return modules;
        }
        String lower = keyword.toLowerCase(Locale.ROOT);
        return modules.stream()
                .filter(module -> module.getName().toLowerCase(Locale.ROOT).contains(lower))
                .collect(Collectors.toList());
    }

    public List<UserSummary> searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return users;
        }
        String lower = keyword.toLowerCase(Locale.ROOT);
        return users.stream()
                .filter(user -> user.getName().toLowerCase(Locale.ROOT).contains(lower)
                        || user.getEmail().toLowerCase(Locale.ROOT).contains(lower))
                .collect(Collectors.toList());
    }
}
