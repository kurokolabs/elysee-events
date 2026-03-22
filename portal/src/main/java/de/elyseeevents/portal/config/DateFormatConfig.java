package de.elyseeevents.portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DateFormatConfig {

    @Bean("dateUtil")
    public DateUtil dateUtil() {
        return new DateUtil();
    }

    public static class DateUtil {
        public String format(String isoDate) {
            if (isoDate == null || isoDate.length() < 10) return "-";
            String date = isoDate.substring(0, 10); // "2026-03-22"
            String[] parts = date.split("-");
            if (parts.length != 3) return date;
            return parts[2] + "." + parts[1] + "." + parts[0]; // "22.03.2026"
        }
    }
}
