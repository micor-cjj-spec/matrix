package single.cjj.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.scheduler.dto.ExecutorHeartbeatRequest;
import single.cjj.scheduler.dto.ExecutorRegisterRequest;
import single.cjj.scheduler.entity.MatrixSchedulerExecutor;
import single.cjj.scheduler.entity.MatrixSchedulerExecutorInstance;
import single.cjj.scheduler.entity.MatrixSchedulerHandler;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutorInstanceMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutorMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerHandlerMapper;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExecutorRegistryService {

    private final MatrixSchedulerExecutorMapper executorMapper;
    private final MatrixSchedulerExecutorInstanceMapper instanceMapper;
    private final MatrixSchedulerHandlerMapper handlerMapper;
    private final long offlineSeconds;

    public ExecutorRegistryService(MatrixSchedulerExecutorMapper executorMapper,
                                   MatrixSchedulerExecutorInstanceMapper instanceMapper,
                                   MatrixSchedulerHandlerMapper handlerMapper,
                                   @Value("${matrix.scheduler.executor.offline-seconds:90}") long offlineSeconds) {
        this.executorMapper = executorMapper;
        this.instanceMapper = instanceMapper;
        this.handlerMapper = handlerMapper;
        this.offlineSeconds = Math.max(30, offlineSeconds);
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerExecutor register(ExecutorRegisterRequest request) {
        String executorCode = request.executorCode().trim();
        LocalDateTime now = LocalDateTime.now();

        MatrixSchedulerExecutor executor = findExecutor(executorCode);
        if (executor == null) {
            executor = new MatrixSchedulerExecutor();
            executor.setFid(IdWorker.getId());
            executor.setFexecutorCode(executorCode);
            executor.setFexecuteType("MQ");
            executor.setFserviceName(executorCode);
            executor.setFcreateTime(now);
        }
        executor.setFexecutorName(request.executorName().trim());
        executor.setFstatus("ONLINE");
        executor.setFlastHeartbeatTime(now);
        executor.setFupdateTime(now);
        if (findExecutor(executorCode) == null) {
            executorMapper.insert(executor);
        } else {
            executorMapper.updateById(executor);
        }

        upsertInstance(executorCode, request.instanceId().trim(),
                request.maxConcurrency() == null ? 10 : request.maxConcurrency(), 0, now);
        syncHandlers(executorCode, request.handlers(), now);
        return executor;
    }

    @Transactional(rollbackFor = Exception.class)
    public void heartbeat(ExecutorHeartbeatRequest request) {
        String executorCode = request.executorCode().trim();
        LocalDateTime now = LocalDateTime.now();
        MatrixSchedulerExecutor executor = findExecutor(executorCode);
        if (executor == null) {
            throw new IllegalArgumentException("执行器尚未注册: " + executorCode);
        }
        executor.setFstatus("ONLINE");
        executor.setFlastHeartbeatTime(now);
        executor.setFupdateTime(now);
        executorMapper.updateById(executor);

        MatrixSchedulerExecutorInstance instance = findInstance(executorCode, request.instanceId().trim());
        if (instance == null) {
            throw new IllegalArgumentException("执行器实例尚未注册: " + request.instanceId());
        }
        instance.setFstatus("ONLINE");
        instance.setFrunningCount(request.runningCount() == null ? 0 : request.runningCount());
        instance.setFlastHeartbeatTime(now);
        instance.setFupdateTime(now);
        instanceMapper.updateById(instance);
    }

    public List<MatrixSchedulerExecutor> listExecutors() {
        return executorMapper.selectList(new LambdaQueryWrapper<MatrixSchedulerExecutor>()
                .orderByDesc(MatrixSchedulerExecutor::getFlastHeartbeatTime));
    }

    public List<MatrixSchedulerExecutorInstance> listInstances(String executorCode) {
        return instanceMapper.selectList(new LambdaQueryWrapper<MatrixSchedulerExecutorInstance>()
                .eq(MatrixSchedulerExecutorInstance::getFexecutorCode, executorCode)
                .orderByDesc(MatrixSchedulerExecutorInstance::getFlastHeartbeatTime));
    }

    public List<MatrixSchedulerHandler> listHandlers(String executorCode) {
        return handlerMapper.selectList(new LambdaQueryWrapper<MatrixSchedulerHandler>()
                .eq(MatrixSchedulerHandler::getFexecutorCode, executorCode)
                .eq(MatrixSchedulerHandler::getFstatus, "ENABLED")
                .orderByAsc(MatrixSchedulerHandler::getFhandlerCode));
    }

    public void validateHandler(String executorCode, String handlerCode) {
        Long count = handlerMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerHandler>()
                .eq(MatrixSchedulerHandler::getFexecutorCode, executorCode)
                .eq(MatrixSchedulerHandler::getFhandlerCode, handlerCode)
                .eq(MatrixSchedulerHandler::getFstatus, "ENABLED"));
        if (count == null || count == 0) {
            throw new IllegalArgumentException("执行器未注册该处理器: " + executorCode + "/" + handlerCode);
        }
    }

    @Scheduled(fixedDelayString = "${matrix.scheduler.executor.offline-scan-ms:30000}")
    @Transactional(rollbackFor = Exception.class)
    public void markOfflineInstances() {
        LocalDateTime deadline = LocalDateTime.now().minusSeconds(offlineSeconds);
        List<MatrixSchedulerExecutorInstance> expired = instanceMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerExecutorInstance>()
                        .eq(MatrixSchedulerExecutorInstance::getFstatus, "ONLINE")
                        .lt(MatrixSchedulerExecutorInstance::getFlastHeartbeatTime, deadline));
        Set<String> touchedExecutors = new HashSet<>();
        for (MatrixSchedulerExecutorInstance instance : expired) {
            instance.setFstatus("OFFLINE");
            instance.setFupdateTime(LocalDateTime.now());
            instanceMapper.updateById(instance);
            touchedExecutors.add(instance.getFexecutorCode());
        }
        for (String executorCode : touchedExecutors) {
            Long online = instanceMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerExecutorInstance>()
                    .eq(MatrixSchedulerExecutorInstance::getFexecutorCode, executorCode)
                    .eq(MatrixSchedulerExecutorInstance::getFstatus, "ONLINE"));
            if (online == null || online == 0) {
                MatrixSchedulerExecutor executor = findExecutor(executorCode);
                if (executor != null) {
                    executor.setFstatus("OFFLINE");
                    executor.setFupdateTime(LocalDateTime.now());
                    executorMapper.updateById(executor);
                }
            }
        }
    }

    private void upsertInstance(String executorCode,
                                String instanceId,
                                int maxConcurrency,
                                int runningCount,
                                LocalDateTime now) {
        MatrixSchedulerExecutorInstance instance = findInstance(executorCode, instanceId);
        boolean create = instance == null;
        if (create) {
            instance = new MatrixSchedulerExecutorInstance();
            instance.setFid(IdWorker.getId());
            instance.setFexecutorCode(executorCode);
            instance.setFinstanceId(instanceId);
            instance.setFcreateTime(now);
        }
        instance.setFstatus("ONLINE");
        instance.setFmaxConcurrency(maxConcurrency);
        instance.setFrunningCount(runningCount);
        instance.setFlastHeartbeatTime(now);
        instance.setFupdateTime(now);
        if (create) {
            instanceMapper.insert(instance);
        } else {
            instanceMapper.updateById(instance);
        }
    }

    private void syncHandlers(String executorCode,
                              List<ExecutorRegisterRequest.HandlerRequest> requests,
                              LocalDateTime now) {
        Set<String> currentCodes = new HashSet<>();
        for (ExecutorRegisterRequest.HandlerRequest request : requests) {
            String handlerCode = request.handlerCode().trim();
            currentCodes.add(handlerCode);
            MatrixSchedulerHandler handler = handlerMapper.selectOne(
                    new LambdaQueryWrapper<MatrixSchedulerHandler>()
                            .eq(MatrixSchedulerHandler::getFexecutorCode, executorCode)
                            .eq(MatrixSchedulerHandler::getFhandlerCode, handlerCode)
                            .last("LIMIT 1"));
            boolean create = handler == null;
            if (create) {
                handler = new MatrixSchedulerHandler();
                handler.setFid(IdWorker.getId());
                handler.setFexecutorCode(executorCode);
                handler.setFhandlerCode(handlerCode);
                handler.setFcreateTime(now);
            }
            handler.setFhandlerName(request.handlerName().trim());
            handler.setFstatus("ENABLED");
            handler.setFupdateTime(now);
            if (create) {
                handlerMapper.insert(handler);
            } else {
                handlerMapper.updateById(handler);
            }
        }

        List<MatrixSchedulerHandler> existing = handlerMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerHandler>()
                        .eq(MatrixSchedulerHandler::getFexecutorCode, executorCode));
        for (MatrixSchedulerHandler handler : existing) {
            if (!currentCodes.contains(handler.getFhandlerCode()) && "ENABLED".equals(handler.getFstatus())) {
                handler.setFstatus("DISABLED");
                handler.setFupdateTime(now);
                handlerMapper.updateById(handler);
            }
        }
    }

    private MatrixSchedulerExecutor findExecutor(String executorCode) {
        return executorMapper.selectOne(new LambdaQueryWrapper<MatrixSchedulerExecutor>()
                .eq(MatrixSchedulerExecutor::getFexecutorCode, executorCode)
                .last("LIMIT 1"));
    }

    private MatrixSchedulerExecutorInstance findInstance(String executorCode, String instanceId) {
        return instanceMapper.selectOne(new LambdaQueryWrapper<MatrixSchedulerExecutorInstance>()
                .eq(MatrixSchedulerExecutorInstance::getFexecutorCode, executorCode)
                .eq(MatrixSchedulerExecutorInstance::getFinstanceId, instanceId)
                .last("LIMIT 1"));
    }
}
