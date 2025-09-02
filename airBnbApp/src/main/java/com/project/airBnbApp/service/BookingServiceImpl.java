package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.BookingDto;
import com.project.airBnbApp.dto.BookingRequest;
import com.project.airBnbApp.dto.GuestDto;
import com.project.airBnbApp.entity.*;
import com.project.airBnbApp.entity.enums.BookingStatus;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.exception.UnAuthorisedException;
import com.project.airBnbApp.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository  roomRepository;
    private final InventoryRepository inventoryRepository;
    private final GuestRepository guestRepository;
    private final CheckOutService  checkOutService;

    @Value("${frontend.url}")
    private String frontendUrl;

    ModelMapper modelMapper = new ModelMapper();
    @Override
    @Transactional
    public BookingDto intialiseBooking(BookingRequest bookingRequest) {

        log.info("intialiseBooking for hotel:{},room :{},date :{}-{} ",bookingRequest.getHotelId()
                ,bookingRequest.getRoomId(),bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate());

        Hotel hotel= hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID:"+bookingRequest.getHotelId()));
        Room room= roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID:"+bookingRequest.getRoomId()));
        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(room.getId(), bookingRequest.getCheckInDate()
                , bookingRequest.getCheckOutDate(),bookingRequest.getRoomsCount());

        long daysCount= ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate())+1;
        if(daysCount!=inventoryList.size()){
            throw new IllegalStateException("Hey no hotel available");
        }

        //Reserve the room/update the bookedCount of inventory

        for(Inventory inventory:inventoryList){
            inventory.setReservedCount(inventory.getBookedCount()+bookingRequest.getRoomsCount());
        }
        inventoryRepository.saveAll(inventoryList);

        //create booking


        // TODO: calculate dynamic amount

        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomCount(bookingRequest.getRoomsCount())
                .amount(BigDecimal.valueOf(100))
                .build();
       booking= bookingRepository.save(booking);
       return modelMapper.map(booking,BookingDto.class);
    }

    @Override
    @Transactional
    public BookingDto addGuests(long bookingId, List<GuestDto> guestDtoList) {
        log.info("Adding Guest for booking with id:{}",bookingId);
        Booking booking= bookingRepository.findById(bookingId)
        .orElseThrow(()-> new ResourceNotFoundException("Booking not found with id:{}"+bookingId));

        User user=getCurrentUser();
        if(!user.equals(booking.getUser())){
            throw new UnAuthorisedException("Booking does not belongs to id:"+user.getId());
        }

        if(hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking has expired");
        }
        if(booking.getBookingStatus()!=BookingStatus.RESERVED){
            throw new IllegalStateException("Cannot add guest booking is not under RESERVED Status");
        }
        for(GuestDto guestDto:guestDtoList){
            Guest guest= modelMapper.map(guestDto,Guest.class);
            guest.setUser(getCurrentUser());
            guest=guestRepository.save(guest);
            booking.getGuests().add(guest);
        }
        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        booking=bookingRepository.save(booking);
        return modelMapper.map(booking,BookingDto.class);
    }


    @Override
    @Transactional
    public String initiatePayments(Long bookingId) {
        Booking booking=bookingRepository.findById(bookingId)
                .orElseThrow(()-> new ResourceNotFoundException("Booking not found with id:"+bookingId));
        User user=getCurrentUser();
        if(!user.equals(booking.getUser())){
            throw new UnAuthorisedException("Booking does not belongs to id:"+user.getId());
        }
        if(hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking has expired");
        }
        String sessionUrl=checkOutService.getCheckoutSession(booking,
                frontendUrl+"/payment/success",frontendUrl+"/payment/failure");

        booking.setBookingStatus(BookingStatus.PAYMENTS_PENDING);

        return sessionUrl;
    }

    public boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    public User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}
