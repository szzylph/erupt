package xyz.demo.erupt.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import xyz.erupt.core.annotation.EruptScan;
import xyz.erupt.core.constant.EruptConst;
import xyz.erupt.core.service.EruptApplication;

@SpringBootApplication
@ComponentScan({EruptConst.BASE_PACKAGE, "xyz.demo.erupt"})
@EntityScan({EruptConst.BASE_PACKAGE, "xyz.demo.erupt"})
@EruptScan({EruptConst.BASE_PACKAGE, "xyz.demo.erupt"})
public class DemoApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        EruptApplication.run(DemoApplication.class, args);
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DemoApplication.class);
    }

}
