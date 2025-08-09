package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.RoomDto;
import com.project.airBnbApp.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImplementation implements RoomService{

    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    @Override
    public RoomDto CreateNewRoom(Long hotelId,RoomDto roomDto) {
        log.info("Creating new room");

        return null;
    }

    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        return List.of();
    }

    @Override
    public RoomDto getRoomById(Long roomId) {
        return null;
    }

    @Override
    public void deleteRoomById(Long roomId) {

    }
}
