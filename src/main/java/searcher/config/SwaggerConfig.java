package searcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spring.web.plugins.Docket;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

@Configuration
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("searcher"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(
                        new ApiInfo(
                                "Searcher Service",
                                "",
                                "latest",
                                EMPTY,
                                new Contact("Matteo Casu", "", "mattecasu@gmail.com"),
                                EMPTY,
                                EMPTY,
                                newArrayList())
                );
    }
}