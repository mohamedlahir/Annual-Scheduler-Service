package com.laby.annual.scheduler.DTO;

import java.time.LocalDate;

public record AcademicYearOptionDTO(
        LocalDate academicYearStart,
        LocalDate academicYearEnd
) {
}
