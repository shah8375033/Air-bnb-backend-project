package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.RoomDto;

import java.util.List;

public interface RoomService {
    RoomDto CreateNewRoom(Long hotelId,RoomDto roomDto);
    List<RoomDto> getAllRoomsInHotel(Long hotelId);
    RoomDto getRoomById(Long roomId);
    void deleteRoomById(Long roomId);
}
