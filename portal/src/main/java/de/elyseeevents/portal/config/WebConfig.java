package de.elyseeevents.portal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public class WebConfig implements WebMvcConfigurer {

    @org.springframework.beans.factory.annotation.Value("${app.website.path:../elysee-events/}")
    private String websitePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Portal assets
        registry.addResourceHandler("/portal/css/**")
                .addResourceLocations("classpath:/static/portal/css/");
        registry.addResourceHandler("/portal/js/**")
                .addResourceLocations("classpath:/static/portal/js/");
        registry.addResourceHandler("/portal/fonts/**")
                .addResourceLocations("classpath:/static/portal/fonts/");
        registry.addResourceHandler("/portal/img/**")
                .addResourceLocations("classpath:/static/portal/img/");

        String base = websitePath.endsWith("/") ? "file:" + websitePath : "file:" + websitePath + "/";
        registry.addResourceHandler("/*.html", "/*.svg", "/*.json", "/*.txt", "/*.xml")
                .addResourceLocations(base);
        registry.addResourceHandler("/css/**")
                .addResourceLocations(base + "css/");
        registry.addResourceHandler("/img/**")
                .addResourceLocations(base + "img/");
    }
}
