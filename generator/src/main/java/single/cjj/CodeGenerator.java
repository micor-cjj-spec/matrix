package single.cjj;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;

public class CodeGenerator {

    public static void generator(String dbName, String dbUser, String dbPassword, String module, String table) {

        // 项目根路径
        String projectPath = System.getProperty("user.dir");

        // 模块 Java 路径
        String outputJavaPath = projectPath + "/" + module + "/src/main/java";

        // Mapper XML 输出路径
        String mapperXmlPath = projectPath + "/" + module + "/src/main/resources/mapper";

        // ✅ 启动代码生成
        FastAutoGenerator.create(
                        "jdbc:mysql://localhost:3306/" + dbName +
                                "?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8", // ✅ 编码修正
                        dbUser,
                        dbPassword
                )
                .globalConfig(builder -> {
                    builder.author("micor") // 作者
                            .outputDir(outputJavaPath) // Java 源码输出路径
                            .disableOpenDir(); // 不自动打开
                })
                .packageConfig(builder -> {
                    builder.parent("single.cjj.bizfi") // 包名根据模块动态生成
                            .entity("entity")
                            .service("service")
                            .serviceImpl("service.impl")
                            .mapper("mapper")
                            .controller("controller")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, mapperXmlPath));
                })
                .strategyConfig(builder -> {
                    builder.addInclude(table) // 指定表
                            .entityBuilder().enableLombok().enableFileOverride()
                            .mapperBuilder().enableBaseResultMap().enableFileOverride()
                            .serviceBuilder().formatServiceFileName("%sService").enableFileOverride()
                            .controllerBuilder().enableRestStyle().enableFileOverride();
                })
                .templateEngine(new VelocityTemplateEngine()) // 模板引擎
                .execute();
    }

    public static void main(String[] args) {
        // 运行参数配置
        String dbName = "bizfi_auth";
        String user = "root";
        String password = "123456";
        String module = "auth-service"; // 模块名为项目文件夹名（必须一致）
        String table = "bizfi_auth_login";

        generator(dbName, user, password, module, table);
    }
}
