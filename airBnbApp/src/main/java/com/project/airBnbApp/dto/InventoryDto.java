package com.project.airBnbApp.dto;

import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.Room;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
public class InventoryDto {
    private Long id;
    private LocalDate date;

    private Integer bookedCount;

    private Integer reservedCount;
    private Integer totalCount;

    private BigDecimal surgeFactor;
    private BigDecimal price;//basePrice*surgeFactor

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean closed;

}
