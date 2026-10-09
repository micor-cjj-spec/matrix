package single.cjj.scheduler.client.core;

import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.util.UUID;

public class SchedulerInstanceIdentity {

    private final String instanceId;

    public SchedulerInstanceIdentity(Environment environment) {
        String serviceName = environment.getProperty("spring.application.name", "matrix-service");
        String port = environment.getProperty("server.port", "0");
        this.instanceId = serviceName + "-" + hostName() + "-" + port + "-"
                + UUID.randomUUID().toString().substring(0, 8);
    }

    public String getInstanceId() {
        return instanceId;
    }

    private String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }
}
