package single.cjj.scheduler.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.scheduler.dto.SchedulerJobRequest;
import single.cjj.scheduler.entity.MatrixSchedulerJob;
import single.cjj.scheduler.service.SchedulerJobService;

@RestController
@RequestMapping("/scheduler/open")
public class SchedulerOpenApiController {

    private final SchedulerJobService jobService;

    public SchedulerOpenApiController(SchedulerJobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/jobs")
    public ApiResponse<MatrixSchedulerJob> create(
            @RequestHeader("X-Source-Service") String sourceService,
            @RequestHeader("X-Request-Id") String requestId,
            @Valid @RequestBody SchedulerJobRequest request) {
        return ApiResponse.success(jobService.create(
                request,
                "OPEN_API",
                sourceService,
                requestId
        ));
    }
}
