package com.xavopls.ibkr_dashboard.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class DateFormatConfig implements Converter<String, LocalDate> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public LocalDate convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return LocalDate.parse(source, DATE_FORMAT);
    }
}
