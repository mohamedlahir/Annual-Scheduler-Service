package com.laby.annual.scheduler.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TutorLeaveRequestDTO {

	@NotNull
	private LocalDate fromDate;

	@NotNull
	private LocalDate toDate;

	private String reason;
}

