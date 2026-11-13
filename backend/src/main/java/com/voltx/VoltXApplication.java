package com.voltx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VoltXApplication {

    public static void main(String[] args) {
        // All business times (bookings, operating hours, peak pricing) are Indian Standard Time.
        TimeZone.setDefault(TimeZone.getTimeZone(System.getProperty("voltx.timezone", "Asia/Kolkata")));
        SpringApplication.run(VoltXApplication.class, args);
    }
}
