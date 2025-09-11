package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.GuestDto;
import com.project.airBnbApp.entity.Guest;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.repository.GuestRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.project.airBnbApp.utils.AppUtils.getCurrentUser;

@Service
@Slf4j
public class GuestServiceImplementation implements GuestService {
    private final ModelMapper modelMapper;
    private final GuestRepository guestRepository;

    public GuestServiceImplementation(ModelMapper modelMapper, GuestRepository guestRepository) {
        this.modelMapper = modelMapper;
        this.guestRepository = guestRepository;
    }

    @Override
    @Transactional
    public GuestDto addNewGuest(GuestDto guestDto) {
        log.info("Adding new guest:{}",guestDto);
        User user=getCurrentUser();
        Guest guest=modelMapper.map(guestDto,Guest.class);
        guest.setUser(user);
        Guest savedGuest=guestRepository.save(guest);
        log.info("guest added with ID:{}",savedGuest.getId());
        return modelMapper.map(savedGuest,GuestDto.class);
    }

    @Override
    @Transactional
    public List<GuestDto> getAllGuests() {
        User user=getCurrentUser();
        log.info("Getting all guests of user with id:{}",user.getId());
        List<Guest> guests=guestRepository.findByUser(user);
        return guests
                .stream()
                .map(guest->modelMapper.map(guest,GuestDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateGuest(Long guestId, GuestDto guestDto) {
        log.info("Updating guest with guestId:{}",guestId);
        Guest guest=guestRepository.findById(guestId)
                .orElseThrow(()->new ResourceNotFoundException("Guest not found with guest Id:{}"+guestId));
        User user=getCurrentUser();
        if (!user.getId().equals(guest.getUser().getId())) {
            throw new AccessDeniedException("You are not the user of this guest with id:"+guestId);
        }
        modelMapper.map(guestDto,Guest.class);
        guest.setId(guestId);
        guest.setUser(user);
        guestRepository.save(guest);
        log.info("Guest with Id:{} updated successfully",guestId);
    }

    @Override
    @Transactional
    public void deleteGuest(Long guestId) {
        log.info("Deleting guest with guestId:{}",guestId);
        Guest guest=guestRepository.findById(guestId)
                .orElseThrow(()->new ResourceNotFoundException("Guest not found with guest Id:{}"+guestId));
        User user=getCurrentUser();
        if (!user.getId().equals(guest.getUser().getId())) {
            throw new AccessDeniedException("You are not the user of this guest with id:"+guestId);
        }
        guestRepository.deleteById(guestId);
        log.info("Guest with Id:{} deleted successfully",guestId);
    }

}
