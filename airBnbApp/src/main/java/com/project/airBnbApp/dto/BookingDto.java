package com.project.airBnbApp.dto;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.entity.enums.BookingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
@Data
public class BookingDto {

    private Long id;
    private User user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer roomCount;
    private BookingStatus status;
    private Set<GuestDto> guests;
    private BigDecimal amount;


}
