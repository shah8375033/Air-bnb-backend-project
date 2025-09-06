package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.BookingDto;
import com.project.airBnbApp.dto.BookingRequest;
import com.project.airBnbApp.dto.GuestDto;
import com.project.airBnbApp.dto.HotelReportDto;
import com.project.airBnbApp.entity.*;
import com.project.airBnbApp.entity.enums.BookingStatus;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.exception.UnAuthorisedException;
import com.project.airBnbApp.repository.*;
import com.project.airBnbApp.strategy.PricingService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.stripe.exception.StripeException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.project.airBnbApp.utils.AppUtils.getCurrentUser;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final GuestRepository guestRepository;
    private final CheckOutService checkOutService;
    private final PricingService pricingService;

    @Value("${frontend.url}")
    private String frontendUrl;

    ModelMapper modelMapper = new ModelMapper();

    @Override
    @Transactional
    public BookingDto intialiseBooking(BookingRequest bookingRequest) {

        log.info("intialiseBooking for hotel:{},room :{},date :{}-{} ", bookingRequest.getHotelId()
                , bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());

        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID:" + bookingRequest.getHotelId()));
        Room room = roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID:" + bookingRequest.getRoomId()));
        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(room.getId(), bookingRequest.getCheckInDate()
                , bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());

        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate()) + 1;
        if (daysCount != inventoryList.size()) {
            throw new IllegalStateException("Hey no hotel available");
        }

        //Reserve the room/update the bookedCount of inventory
        inventoryRepository.initBooking(room.getId(),
                bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(),
                bookingRequest.getRoomsCount());

// for loop got replace with JPQL Query
//        for (Inventory inventory : inventoryList) {
//            inventory.setReservedCount(inventory.getBookedCount() + bookingRequest.getRoomsCount());
//        }
//        inventoryRepository.saveAll(inventoryList);

        //create booking


        // TODO: calculate dynamic amount
        BigDecimal priceForOneRoom = pricingService.calculateTotalPrice(inventoryList);
        BigDecimal totalPrice=priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));

        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomCount(bookingRequest.getRoomsCount())
                .amount(totalPrice)
                .build();
        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }

    @Override
    @Transactional
    public BookingDto addGuests(long bookingId, List<GuestDto> guestDtoList) {
        log.info("Adding Guest for booking with id:{}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id:{}" + bookingId));

        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belongs to id:" + user.getId());
        }

        if (hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking has expired");
        }
        if (booking.getBookingStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Cannot add guest booking is not under RESERVED Status");
        }
        for (GuestDto guestDto : guestDtoList) {
            Guest guest = modelMapper.map(guestDto, Guest.class);
            guest.setUser(getCurrentUser());
            guest = guestRepository.save(guest);
            booking.getGuests().add(guest);
        }
        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }


    @Override
    @Transactional
    public String initiatePayments(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id:" + bookingId));
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belongs to id:" + user.getId());
        }
        if (hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking has expired");
        }
        String sessionUrl = checkOutService.getCheckoutSession(booking,
                frontendUrl + "/payment/success", frontendUrl + "/payment/failure");

        booking.setBookingStatus(BookingStatus.PAYMENTS_PENDING);

        return sessionUrl;
    }

    @Override
    @Transactional
    public void capturePayment(Event event) {
        log.info("Received Stripe event: type={}, id={}", event.getType(), event.getId());

        if ("checkout.session.completed".equals(event.getType())) {
            try {
                // modern deserialization with fallback
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                StripeObject stripeObject = deserializer.getObject().orElse(deserializer.deserializeUnsafe());

                // Check if the object is null
                if (stripeObject == null) {
                    log.error("Failed to deserialize Stripe object for event: {}", event.getId());
                    return;
                }

                //  Verify it's a Session object
                if (!(stripeObject instanceof Session)) {
                    log.error("Expected Session object but got: {}", stripeObject.getClass().getSimpleName());
                    return;
                }

                //  Cast to Session
                Session session = (Session) stripeObject;

                String sessionId = session.getId();
                if (session.getId() == null) {
                    log.error("Session ID is null for event: {}", event.getId());
                    return;
                }

                log.info("Processing session: id={}, payment_status={}",
                        session.getId(), session.getPaymentStatus());

                //  Process the completed session
                Booking booking = bookingRepository.findByPaymentSessionId(sessionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with with sessionId:" + sessionId));

                booking.setBookingStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);

                inventoryRepository.findAndLockReservedInventory(
                        booking.getRoom().getId(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate(),
                        booking.getRoomCount());
                inventoryRepository.confirmBooking(
                        booking.getRoom().getId(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate(),
                        booking.getRoomCount()
                );
                log.info("Successfully confirmed the booking for booking ID:{}", booking.getId());

            } catch (Exception e) {
                log.error("Error processing checkout.session.completed event: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process payment confirmation", e);

            }
        }
        else{
            log.warn("Unhandled event type:{}", event.getType());
        }
    }

    @Override
    @Transactional
    public void cancelPayments(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id:" + bookingId));
        User user = getCurrentUser();
        if (!user.getId().equals(booking.getUser().getId())) {
            throw new UnAuthorisedException("Booking does not belongs to id:" + user.getId());
        }
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking does not belongs to id:" + booking.getId());
        }
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        inventoryRepository.cancelBooking(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomCount()
        );
        //handle the refund
        log.info("Successfully cancelled the booking for booking ID:{}", booking.getId());

        try {
            Session session = Session.retrieve(booking.getPaymentSessionId());
            RefundCreateParams refundParams=RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent())
                    .build();
            Refund.create(refundParams);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    @Transactional
    public String getBookingStatus(Long bookingId) {
        User user = getCurrentUser();
        Booking booking=bookingRepository.findById(bookingId).orElseThrow(()->new ResourceNotFoundException("Booking not found with id:" + bookingId));
        if (!user.getId().equals(booking.getUser().getId())) {
            throw new UnAuthorisedException("Booking does not belongs to id:" + user.getId());
        }
        return booking.getBookingStatus().name();
    }

    @Override
    @Transactional
    public List<BookingDto> getBookingByHotelId(Long hotelId){
        Hotel hotel=hotelRepository.findById(hotelId)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:" + hotelId));
        User user = getCurrentUser();
        log.info("Getting booking for hotel by hotelId:{}",hotel.getId());
        if(!user.getId().equals(hotel.getOwner().getId())) throw new AccessDeniedException("You are not the Owner of the Hotel with hotelId:"+hotelId);
        List<Booking> bookings=bookingRepository.findByHotel(hotel);
        return bookings
                .stream().map((element) -> modelMapper.map(element, BookingDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HotelReportDto getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        Hotel hotel=hotelRepository.findById(hotelId)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:" + hotelId));
        User user = getCurrentUser();
        log.info("Getting booking for hotel by hotelId:{}",hotel.getId());
        if(!user.getId().equals(hotel.getOwner().getId())) throw new AccessDeniedException("You are not the Owner of the Hotel with hotelId:"+hotelId);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<Booking> bookings=bookingRepository.findByHotelAndCreatedAtBetween(hotel, startDateTime, endDateTime);
        long totalConfirmBookings=bookings.stream()
                .filter(booking -> booking.getBookingStatus()==BookingStatus.CONFIRMED)
                .count();
        BigDecimal totalRevenveOfConfirmedBookings=bookings
                .stream()
                .filter(booking -> booking.getBookingStatus()==BookingStatus.CONFIRMED)
                .map(Booking :: getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgRevenue=totalConfirmBookings == 0 ? BigDecimal.ZERO : totalRevenveOfConfirmedBookings
                .divide(BigDecimal.valueOf(totalConfirmBookings), RoundingMode.HALF_DOWN);
        return new HotelReportDto(totalConfirmBookings, totalRevenveOfConfirmedBookings, avgRevenue) ;
    }

    @Override
    @Transactional
    public List<BookingDto> getMyBookings() {
        User user=getCurrentUser();
        return bookingRepository.getByUser(user)
                .stream()
                .map((element) -> modelMapper.map(element, BookingDto.class))
                .collect(Collectors.toList());

    }

    public boolean hasBookingExpired (Booking booking){
            return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
        }




}