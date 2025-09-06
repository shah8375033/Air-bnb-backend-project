package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.HotelDto;
import com.project.airBnbApp.dto.HotelInfoDto;
import com.project.airBnbApp.dto.RoomDto;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.entity.Room;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.exception.UnAuthorisedException;
import com.project.airBnbApp.repository.HotelRepository;
import com.project.airBnbApp.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.project.airBnbApp.utils.AppUtils.getCurrentUser;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImplementation implements HotelService{
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;
    private final RoomRepository roomRepository;

    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        log.info("Creating new Hotel with name: {}", hotelDto.getName());
        Hotel hotel= modelMapper.map(hotelDto, Hotel.class);
        hotel.setActive(false);
        User user=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        hotel.setOwner(user);
        log.info("Hotel created with hotelId:{}"+hotel.getId());
        return modelMapper.map(hotelRepository.save(hotel), HotelDto.class);

    }

    @Override
    public HotelDto getHotelById(Long id) {
        log.info("Getting hotel with id :{}",id);
        Hotel hotel= hotelRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("Hotel not found with Id:{}"+id));

        User user=(User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.getId().equals(hotel.getOwner().getId())) {
            throw new UnAuthorisedException("This user does not owns this hotel with id:"+id);
        }
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto updateHotelById(Long id, HotelDto hotelDto) {
        log.info("Updating hotel with id :{}",id);
        Hotel hotel=hotelRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("Hotel not found with Id :{}"+id));
        modelMapper.map(hotelDto, hotel);
        User user=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.getId().equals(hotel.getOwner().getId())) throw new UnAuthorisedException("This user does not owns this hotel with id:" + id);
        hotel.setId(id);
        hotelRepository.save(hotel);
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Transactional
    @Override
    public void deleteHotelById(Long id) {
        Hotel hotel=hotelRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("Hotel not found with Id :{}"+id));

        User user=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.getId().equals(hotel.getOwner().getId())) {
            throw new UnAuthorisedException("This user does not owns this hotel with id:"+id);
        }

        for(Room room:hotel.getRooms()){
            inventoryService.deleteAllInventories(room);
            roomRepository.deleteById(room.getId());
        }
        hotelRepository.deleteById(id);

    }

    @Transactional
    @Override
    public void activateHotel(Long id) {
        log.info("Activating hotel with id :{}",id);
        Hotel hotel=hotelRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("Hotel not found with Id :{}"+id));

        User user=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.getId().equals(hotel.getOwner().getId())) {
            throw new UnAuthorisedException("This user does not owns this hotel with id:"+id);
        }

        hotel.setActive(true);
        //assuming only do it once
        for(Room room:hotel.getRooms()){
            inventoryService.initialiseRoomForAYear(room);
        }

    }


    //public method
    @Override
    public HotelInfoDto getHotelInfoById(Long hotelId) {
        Hotel hotel=hotelRepository
                .findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with Id :{}"+hotelId));
        List<RoomDto> rooms=hotel.getRooms()
                .stream()
                .map((element)-> modelMapper.map(element,RoomDto.class))
                .toList();
        return new HotelInfoDto(modelMapper.map(hotel,HotelDto.class),rooms);
    }

    @Override
    public List<HotelDto> getAllHotels() {
        User user=getCurrentUser();
        log.info("Getting all hotel for the admin user with ID:{}",user.getId());
        List<Hotel> hotels=hotelRepository.findByOwner(user);
        return hotels
                .stream()
                .map((element)-> modelMapper.map(element,HotelDto.class))
                .collect(Collectors.toList());
    }
}
