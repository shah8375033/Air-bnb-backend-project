package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.HotelDto;
import com.project.airBnbApp.entity.Hotel;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImplementation implements HotelService{
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        log.info("Creating new Hotel with name: {}", hotelDto.getName());
        Hotel hotel= modelMapper.map(hotelDto, Hotel.class);
        hotel.setActive(false);
        log.info("Hotel created with hotelId:{}"+hotel.getId());
        return modelMapper.map(hotelRepository.save(hotel), HotelDto.class);

    }

    @Override
    public HotelDto getHotelById(Long id) {
        log.info("Getting hotel with id :{}",id);
        Hotel hotel= hotelRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("Hotel not found with Id:{}"+id));
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto updateHotelById(Long id, HotelDto hotelDto) {
        log.info("Updating hotel with id :{}",id);
        Hotel hotel=hotelRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("Hotel not found with Id :{}"+id));
        modelMapper.map(hotelDto, hotel);
        hotel.setId(id);
        hotelRepository.save(hotel);
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public void deleteHotelById(Long id) {
        boolean exists=hotelRepository.existsById(id);
        if(!exists)throw new ResourceNotFoundException("Hotel not found eith ID:{}"+id);
        hotelRepository.deleteById(id);

    }

    @Override
    public void activateHotel(Long id) {
        log.info("Activating hotel with id :{}",id);
        Hotel hotel=hotelRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("Hotel not found with Id :{}"+id));
        hotel.setActive(true);

    }

}
