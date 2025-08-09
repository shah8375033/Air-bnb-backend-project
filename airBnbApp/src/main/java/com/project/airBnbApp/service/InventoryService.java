package com.project.airBnbApp.service;

import com.project.airBnbApp.entity.Room;

public interface InventoryService {
    void initialiseRoomForAYear(Room room);
    void deleteFutureInventories(Room room);
}
