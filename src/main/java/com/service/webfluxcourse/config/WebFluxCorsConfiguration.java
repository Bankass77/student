package com.service.webfluxcourse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxCorsConfiguration implements WebFluxConfigurer {

    /*    @Override
        public  void addCorsMappings(CorsRegistry registry){
            registry.addMapping("/**")
                    .allowedOrigins("http://localhost:8080")
                    .allowedMethods("POST", "PUT","DELETE","GET" )
                    .allowedHeaders("X-USER-ID");
        }
        */

}
