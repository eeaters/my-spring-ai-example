package io.eeaters.event;


import java.util.List;
import java.util.Objects;

public class MemoryRepository {


    public List<Schema.Solution> getSolutions(Schema.Type type) {
        return solutionList.stream().filter(s -> s.type() == type).toList();
    }


    public Schema.Solution getSolution(Integer id) {
        return solutionList.stream().filter(s -> Objects.equals(s.id(), id)).toList().getFirst();
    }

    private static List<Schema.Solution> solutionList = List.of(
            new Schema.Solution(
                    1,
                    "优化数据库连接池",
                    Schema.Type.DB,
                    "调整数据库连接池参数，增加最大连接数并优化连接回收机制",
                    List.of(
                            "检查当前连接池配置",
                            "调整最大连接数参数",
                            "优化连接超时设置",
                            "重启应用服务"
                    ),
                    "Low",
                    List.of(
                            "已检查连接池配置，当前最大连接数为50",
                            "已将最大连接数调整为100",
                            "已将连接超时时间从30分钟调整为15分钟",
                            "已重启应用服务，新配置生效"
                    )
            ),
            new Schema.Solution(
                    2,
                    "识别并关闭空闲连接",
                    Schema.Type.DB,
                    "查找并关闭长时间空闲的数据库连接，释放资源",
                    List.of(
                            "执行数据库查询识别空闲连接",
                            "关闭超过30分钟的空闲连接",
                            "记录关闭的连接数量"
                    ),
                    "Low",
                    List.of(
                            "已执行数据库查询，发现5个空闲连接",
                            "已成功关闭5个空闲连接",
                            "系统连接数已降低，继续监控中"
                    )
            ),
            new Schema.Solution(
                    3,
                    "扩展数据库资源",
                    Schema.Type.DB,
                    "增加数据库服务器资源或启动读写分离，分散连接压力",
                    List.of(
                            "评估当前数据库负载",
                            "准备额外的数据库实例",
                            "配置读写分离",
                            "迁移部分连接到新实例"
                    ),
                    "Medium",
                    List.of(
                            "已评估数据库负载，当前单实例压力较大",
                            "已准备额外的读库实例",
                            "已配置读写分离，读操作将转发到新实例",
                            "已成功迁移30%的连接到新实例"
                    )
            ),
            new Schema.Solution(
                    4,
                    "识别高CPU进程",
                    Schema.Type.CPU,
                    "查找并分析导致CPU高使用率的进程",
                    List.of(
                            "执行top命令查看进程CPU使用情况",
                            "分析高CPU使用率进程的功能和重要性",
                            "检查是否存在异常或恶意进程"
                    ),
                    "Low",
                    List.of(
                            "已执行top命令，发现Java进程占用CPU 85%",
                            "分析显示该进程为核心业务应用，非异常进程",
                            "建议优化应用代码或增加服务器资源"
                    )
            ),
            new Schema.Solution(
                    5,
                    "优化或重启高负载应用",
                    Schema.Type.CPU,
                    "对高CPU使用的应用进行优化或重启",
                    List.of(
                            "备份应用当前状态和数据",
                            "优化应用配置或代码",
                            "必要时重启应用",
                            "监控重启后的CPU使用情况"
                    ),
                    "Medium",
                    List.of(
                            "已备份应用当前状态和关键数据",
                            "已优化JVM参数配置",
                            "已重启应用服务",
                            "重启后CPU使用率降至45%，继续监控中"
                    )
            ),
            new Schema.Solution(
                    6,
                    "识别内存泄漏",
                    Schema.Type.MEMORY,
                    "检查是否存在内存泄漏问题并定位",
                    List.of(
                            "使用内存分析工具检查进程内存使用",
                            "识别可能存在内存泄漏的应用",
                            "分析内存使用增长趋势"
                    ),
                    "Low",
                    List.of(
                            "已使用内存分析工具检查进程",
                            "发现NodeJS应用存在内存持续增长现象",
                            "分析显示可能存在内存泄漏，建议联系开发团队修复"
                    )
            ),
            new Schema.Solution(
                    7,
                    "清理系统缓存",
                    Schema.Type.MEMORY,
                    "清理系统缓存释放内存空间",
                    List.of(
                            "执行缓存清理命令",
                            "监控内存释放情况",
                            "评估清理效果"
                    ),
                    "Low",
                    List.of(
                            "已执行系统缓存清理命令",
                            "已释放1024MB内存空间",
                            "内存使用率降至65%，继续监控中"
                    )
            ),
            new Schema.Solution(
                    8,
                    "通用系统检查",
                    Schema.Type.COMMON,
                    "执行通用系统检查流程，排查常见问题",
                    List.of(
                            "检查系统日志文件",
                            "检查关键服务状态",
                            "检查网络连接情况",
                            "检查存储空间使用情况"
                    ),
                    "Low",
                    List.of(
                            "已检查系统日志，未发现异常记录",
                            "已确认所有关键服务运行正常",
                            "已检查网络连接，网络状态良好",
                            "已检查存储空间，使用率正常"
                    )
            )
    );


}
