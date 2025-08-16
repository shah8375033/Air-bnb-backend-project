package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.RoomDto;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.Room;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.repository.HotelRepository;
import com.project.airBnbApp.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImplementation implements RoomService{

    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final HotelRepository hotelRepository;
    private final InventoryServiceImplementation inventoryService;

    @Transactional
    @Override
    public RoomDto CreateNewRoom(Long hotelId, RoomDto roomDto) {
        log.info("Creating new room in hotel eith id: {}"+hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with id:"+hotelId));
        Room room=modelMapper.map(roomDto, Room.class);
        room.setHotel(hotel);
        Room savedRoom=roomRepository.save(room);
        if(hotel.getActive()){
            inventoryService.initialiseRoomForAYear(room);
        }
        return modelMapper.map(savedRoom, RoomDto.class);


    }

    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Getting all rooms in hotel with id: {}"+hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with id:"+hotelId));
        return hotel.getRooms()
                .stream()
                .map((element) -> modelMapper.map(element, RoomDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public RoomDto getRoomById(Long roomId) {
        log.info("Getting room with id: {}"+roomId);
        Room room=roomRepository.findById(roomId).orElseThrow(()-> new ResourceNotFoundException("Room not found with id:"+roomId));
        return modelMapper.map(room, RoomDto.class);
    }



    @Transactional
    @Override
    public void deleteRoomById(Long roomId) {
        log.info("Deleting room with id: {}"+roomId);
        Room room=roomRepository
                .findById(roomId)
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with id:"+roomId));
        inventoryService.deleteAllInventories(room);
        roomRepository.deleteById(roomId);

    }
}