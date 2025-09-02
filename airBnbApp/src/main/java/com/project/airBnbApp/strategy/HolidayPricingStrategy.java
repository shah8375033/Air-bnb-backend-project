package com.project.airBnbApp.strategy;

import com.project.airBnbApp.entity.Inventory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class HolidayPricingStrategy implements PricingStrategy{
    private final PricingStrategy wrapped;
    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);
        boolean isTodayHoliday=true;// :Give api call Or array of holiday Or check with local date timr
        if(isTodayHoliday){
            price=price.multiply(BigDecimal.valueOf(1.25));
        }
        return price;
    }
}
