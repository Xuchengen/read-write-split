package com.github.xuchengen.rws;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication(scanBasePackages = "com.github.xuchengen.rws")
@ServletComponentScan
@EnableTransactionManagement
@MapperScan(value = "com.github.xuchengen.rws.dao")
@EnableKnife4j
@EnableSwagger2WebMvc
public class Server {

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("基于ShardingSphere-JDBC实现读写分离")
                        .description("# 基于ShardingSphere-JDBC实现读写分离\n" +
                                "---\n" +
                                "* Spring-boot\n" +
                                "* Hikari\n" +
                                "* Mybatis\n" +
                                "* TkMapper\n" +
                                "* PageHelper")
                        .termsOfServiceUrl("https://www.xuchengen.cn/")
                        .contact(new Contact("徐承恩",
                                "https://www.xuchengen.cn",
                                "mailto://xuchengen@gmail.com"))
                        .version("1.0")
                        .build())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.github.xuchengen.rws.web"))
                .paths(PathSelectors.any())
                .build();
    }
}
