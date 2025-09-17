package io.eeaters.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public interface Schema {

    record Solution(
            @JsonPropertyDescription("解决方案id") Integer id,
            @JsonPropertyDescription("解决方案名称") String name,
            @JsonPropertyDescription("方案类型") Type type,
            @JsonPropertyDescription("解决方案的描述") String description,
            @JsonPropertyDescription("解决的操作步骤") List<String> step,
            @JsonPropertyDescription("风险等级") String level,
            @JsonIgnore List<String> result
    ) {

    }

    enum Type {
        DB,
        MEMORY,
        CPU,
        COMMON
    }

    record SystemStatusInfo(
            @JsonPropertyDescription("连接数") Integer connection,
            @JsonPropertyDescription("CPU使用率, 1-100") Integer cpuUsage,
            @JsonPropertyDescription("内存使用率 , 1-100") Integer memoryUsage) {


        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Integer connection;
            private Integer cpuUsage;
            private Integer memoryUsage;

            public SystemStatusInfo.Builder withConnection(Integer connection) {
                this.connection = connection;
                return this;
            }

            public SystemStatusInfo.Builder withCpuUsage(Integer cpuUsage) {
                this.cpuUsage = cpuUsage;
                return this;
            }

            public SystemStatusInfo.Builder withMemoryUsage(Integer memoryUsage) {
                this.memoryUsage = memoryUsage;
                return this;
            }

            public SystemStatusInfo build() {
                return new SystemStatusInfo(connection, cpuUsage, memoryUsage);
            }
        }
    }


    record RecommendSolutionRequest(
            String alertType,
            String currentStatus
    ) {

    }

    record RecommendSolutionResponse(
           @JsonPropertyDescription("执行结果") List<String> details
    ) {

    }

}
