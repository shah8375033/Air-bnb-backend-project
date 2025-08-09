package com.project.airBnbApp.dto;

import com.project.airBnbApp.entity.HotelContactInfo;
import lombok.Data;
import java.util.List;
@Data
public class HotelDto {
    private Long id;
    private String name;
    private String city;
    private String[] photos;
    private String[] amenities;
    private HotelContactInfo contactInfo;
    private Boolean active;

}
