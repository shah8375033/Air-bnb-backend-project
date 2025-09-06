package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.*;
import com.project.airBnbApp.entity.*;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.exception.UnAuthorisedException;
import com.project.airBnbApp.repository.HotelMinPriceRepository;
import com.project.airBnbApp.repository.InventoryRepository;
import com.project.airBnbApp.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.project.airBnbApp.utils.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImplementation implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;
    private final HotelMinPriceRepository  hotelMinPriceRepository;
    private final RoomRepository roomRepository;

    @Override
    public void initialiseRoomForAYear(Room room) {
        LocalDate today= LocalDate.now();
        LocalDate endDate =today.plusYears(1);
        for(;!today.isAfter(endDate);today=today.plusDays(1)){
            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .reservedCount(0)
                    .city(room.getHotel().getCity())
                    .date(today)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventoryRepository.save(inventory);

        }
    }

    @Override
    public void deleteAllInventories(Room room) {
        LocalDate today= LocalDate.now();
        inventoryRepository.deleteByRoom(room);

    }

//    @Override
//    public Page<HotelDto> searchHotels(HotelSearchRequest hotelSearchRequest) {
//        log.info("searching hotels {} city from {} to {}",hotelSearchRequest.getCity(),hotelSearchRequest.getCity()
//                ,hotelSearchRequest.getStartDate(),hotelSearchRequest.getEndDate());
//        Pageable pageable= PageRequest.of(hotelSearchRequest.getPage(),hotelSearchRequest.getPageSize());
//        long dateCount=
//                ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(),hotelSearchRequest.getEndDate())+1;
//
//        Page<Hotel> hotelPage=inventoryRepository
//                .findHotelsWithAvailableInventory(hotelSearchRequest.getCity(),
//                hotelSearchRequest.getStartDate(),hotelSearchRequest.getEndDate(),hotelSearchRequest.getRoomsCount(),dateCount,pageable);
//        return hotelPage.map((element) -> modelMapper.map(element,HotelDto.class));
//    }
@Override
@Transactional
public Page<HotelPriceDto> searchHotels(HotelSearchRequest hotelSearchRequest) {
    log.info("searching hotels {} city from {} to {}",hotelSearchRequest.getCity(),hotelSearchRequest.getCity()
            ,hotelSearchRequest.getStartDate(),hotelSearchRequest.getEndDate());
    Pageable pageable= PageRequest.of(hotelSearchRequest.getPage(),hotelSearchRequest.getPageSize());
    long dateCount=
            ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(),hotelSearchRequest.getEndDate())+1;

    Page<HotelPriceDto> hotelPage=hotelMinPriceRepository
            .findHotelsWithAvailableInventory(hotelSearchRequest.getCity(),
                    hotelSearchRequest.getStartDate(),hotelSearchRequest.getEndDate(),hotelSearchRequest.getRoomsCount(),dateCount,pageable);
    return hotelPage;
}

    @Override
    @Transactional
    public List<InventoryDto> getAllInventoryByRoom(Long roomId) {
        log.info("Getting all inventory for room with id {}",roomId);
        Room room=roomRepository.findById(roomId)
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID :"+roomId));
        User user=getCurrentUser();
        if (!user.getId().equals(room.getHotel().getOwner().getId())) {
            throw new AccessDeniedException("This user does not owns this hotel with id:"+roomId);
        }
        return inventoryRepository.findByRoomOrderByDate(room)
                .stream()
                .map((element) -> modelMapper.map(element, InventoryDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto) {
        log.info("Updating inventory for room with id {} between date range {}-{}",roomId,
                updateInventoryRequestDto.getStartDate(),updateInventoryRequestDto.getEndDate());
        Room room=roomRepository.findById(roomId)
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID :"+roomId));
        User user=getCurrentUser();
        if (!user.getId().equals(room.getHotel().getOwner().getId())) {
            throw new AccessDeniedException("This user does not owns this hotel with id:"+roomId);
        }
        inventoryRepository.getInventoryAndLockBeforeUpdate(roomId,updateInventoryRequestDto.getStartDate()
                ,updateInventoryRequestDto.getEndDate());

        inventoryRepository.updateInventory(roomId,updateInventoryRequestDto.getStartDate()
                , updateInventoryRequestDto.getEndDate()
                ,updateInventoryRequestDto.getClosed(),
                updateInventoryRequestDto.getSurgeFactor());
    }
}
