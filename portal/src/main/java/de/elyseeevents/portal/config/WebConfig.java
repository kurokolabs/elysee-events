package de.elyseeevents.portal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Portal assets (höchste Priorität)
        registry.addResourceHandler("/portal/css/**")
                .addResourceLocations("classpath:/static/portal/css/");
        registry.addResourceHandler("/portal/js/**")
                .addResourceLocations("classpath:/static/portal/js/");
        registry.addResourceHandler("/portal/fonts/**")
                .addResourceLocations("classpath:/static/portal/fonts/");
        registry.addResourceHandler("/portal/img/**")
                .addResourceLocations("classpath:/static/portal/img/");

        // Statische Website — nur konkrete Dateitypen, nicht /portal/
        registry.addResourceHandler("/*.html", "/*.svg", "/*.json", "/*.txt", "/*.xml")
                .addResourceLocations("file:../elysee-events/");
        registry.addResourceHandler("/css/**")
                .addResourceLocations("file:../elysee-events/css/");
        registry.addResourceHandler("/img/**")
                .addResourceLocations("file:../elysee-events/img/");
    }
}
