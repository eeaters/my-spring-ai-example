package io.eeaters.event;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Random;

public class Tools {

    private static final Random random = new Random();

    private final MemoryRepository memoryRepository = new MemoryRepository();

    @Tool(description = "调用监控系统接口,获取数据库服务器性能指标")
    public Schema.SystemStatusInfo getCurrentStatus() {
        System.out.println("===调用监控系统接口,获取数据库服务器性能指标===");
        int connections = random.nextInt(10, 100);
        int cpuUsage = random.nextInt(1, 100);
        int memoryUsage = random.nextInt(10, 100);

        return Schema.SystemStatusInfo.builder()
                .withConnection(connections)
                .withCpuUsage(cpuUsage)
                .withMemoryUsage(memoryUsage)
                .build();
    }

    @Tool(description = "系统推荐的处置方案")
    public List<Schema.Solution> recommendSolutions(Schema.Type type) {
        System.out.println("===系统推荐的处置方案===");
        return memoryRepository.getSolutions(type);
    }

    @Tool(description = "给予用户自传选择处置方案的入口")
    public Integer userConfirm(@ToolParam(description = "提供给用户选择的选项") List<Integer> request) {
        System.out.println("===给予用户自传选择处置方案的入口===");
        return request.getFirst();
    }


    @Tool(description = "执行处置方案")
    public Schema.RecommendSolutionResponse executeSolution(@ToolParam(description = "处置方案的ID") Integer solutionId) {
        System.out.println("===执行处置方案===");
        return new Schema.RecommendSolutionResponse(memoryRepository.getSolution(solutionId).result());
    }



}
