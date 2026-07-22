package single.cjj.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SchedulerJobRequest {

    private String jobCode;

    @NotBlank(message = "任务名称不能为空")
    private String jobName;

    @NotBlank(message = "Cron 表达式不能为空")
    private String cronExpression;

    private String timezone = "Asia/Shanghai";
    private String tenantId = "default";

    @NotBlank(message = "执行类型不能为空")
    private String executeType = "MQ";

    @NotBlank(message = "执行器编码不能为空")
    private String executorCode;

    @NotBlank(message = "处理器编码不能为空")
    private String handlerCode;

    private String executeParameters = "{}";
    private String concurrencyPolicy = "SKIP";
    private String misfirePolicy = "FIRE_ONCE_NOW";

    @Positive(message = "超时时间必须大于0")
    private Integer timeoutSeconds = 300;

    private Integer retryCount = 0;
    private Integer retryIntervalSeconds = 60;
    private Boolean enabled = true;
}
