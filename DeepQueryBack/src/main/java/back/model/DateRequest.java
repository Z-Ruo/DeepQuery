package back.model;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

import java.time.LocalDate;
@Data
public class DateRequest {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
}
