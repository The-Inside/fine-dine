package com.finedine.riderservice.service;

import com.finedine.riderservice.dto.DeliveryRequestDTO;
import com.finedine.riderservice.dto.RiderRegistrationQueue;
import com.finedine.riderservice.dto.RiderResponse;
import com.finedine.riderservice.entity.Delivery;
import com.finedine.riderservice.entity.Rider;
import com.finedine.riderservice.enums.DeliveryStatus;
import com.finedine.riderservice.enums.Status;
import com.finedine.riderservice.exception.NotFoundException;
import com.finedine.riderservice.exception.UnauthorizedException;
import com.finedine.riderservice.repository.DeliveryRepository;
import com.finedine.riderservice.repository.RiderRepository;
import com.finedine.riderservice.security.SecurityUser;
import com.finedine.riderservice.util.RiderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static com.finedine.riderservice.enums.Availability.AVAILABLE;
import static com.finedine.riderservice.util.CustomMessages.RIDER_OFFLINE;
import static com.finedine.riderservice.util.CustomMessages.RIDER_ONLINE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiderServiceImplTest {

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private RiderMapper riderMapper;

    @Mock
    private  RestTemplate restTemplate;

    @InjectMocks
    private RiderServiceImpl riderService;


    @Test
    void shouldCreateRider(){
        RiderRegistrationQueue riderObject = createRiderObject;

        Rider rider = Rider.builder()
                .id(1L)
                .accountId(riderObject.accountId())
                .email(riderObject.email())
                .externalId(riderObject.externalId())
                .build();

        when(riderMapper.toRider(riderObject)).thenReturn(rider);
        when(riderRepository.save(rider)).thenReturn(rider);

        var result = riderService.createRider(riderObject);

        assertThat(result.getId()).isNotNull();
        assertEquals(rider.getId(), result.getId());
        assertEquals(Status.ONLINE, result.getStatus());
        assertEquals("testuser@gmail.com", result.getEmail());
        assertEquals("randomExternalId", result.getExternalId());

        verify(riderMapper, times(1)).toRider(riderObject);
        verify(riderRepository, times(1)).save(rider);
    }

    @Test
    void shouldCreateDelivery(){
        DeliveryRequestDTO deliveryObject = createDeliveryObject;

        Delivery delivery = Delivery.builder()
                .id(1L)
                .orderId(deliveryObject.orderId())
                .restaurantId(deliveryObject.restaurantId())
                .customerId(deliveryObject.customerId())
                .build();

        when(riderMapper.toDelivery(deliveryObject)).thenReturn(delivery);
        when(deliveryRepository.save(delivery)).thenReturn(delivery);

        var result = riderService.createDelivery(deliveryObject);

        assertEquals(delivery.getId(), result.getId());
        assertEquals(DeliveryStatus.PENDING,result.getStatus());
        assertEquals(deliveryObject.orderId(), result.getOrderId());
        assertEquals(deliveryObject.restaurantId(), result.getRestaurantId());
        assertEquals(deliveryObject.customerId(), result.getCustomerId());

        verify(riderMapper, times(1)).toDelivery(deliveryObject);
        verify(deliveryRepository, times(1)).save(delivery);
    }

    @Test
    void shouldGetUserProfileDetails(){
        SecurityUser securityUser = newSecurityUser;

        Rider rider = newRider;
        when(riderRepository.findByExternalId(securityUser.externalId())).thenReturn(Optional.of(rider));

        Rider result = riderService.myProfile(securityUser);

        assertThat(result).isNotNull();
        assertEquals(rider.getAccountId(), result.getAccountId());
        assertEquals("testuser@gmail.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("externalId", result.getExternalId());

        verify(riderRepository, times(1)).findByExternalId("externalId");
    }

    @Test
    void testUnauthorizedTenantAccess() {
        SecurityUser unauthorizedSecUser = new SecurityUser("unauthorized@gmail.com", "externalId1", List.of());

        Rider mismatchedRider = Rider.builder()
                .email("unauthorized@gmail.com")
                .externalId("externalId2")
                .build();

        when(riderRepository.findByExternalId(unauthorizedSecUser.externalId())).thenReturn(Optional.of(mismatchedRider));

        assertThrows(UnauthorizedException.class, () -> {
            riderService.myProfile(unauthorizedSecUser);
        });

        verify(riderRepository, times(1)).findByExternalId("externalId1");
    }

    @Test
    void testUserWithNoExternalId(){
        SecurityUser securityUser = new SecurityUser(null,null,null);

        when(riderRepository.findByExternalId(securityUser.externalId())).thenReturn(Optional.ofNullable(any(Rider.class)));

        assertThrows(NotFoundException.class, ()-> riderService.myProfile(securityUser));
    }

    @Test
    void testGoOnline(){
        SecurityUser securityUser = newSecurityUser;
        Rider rider = newRider;

        when(riderRepository.findByExternalId(securityUser.externalId())).thenReturn(Optional.of(rider));
        when(riderRepository.save(rider)).thenReturn(rider);

        var result = riderService.goOnline(securityUser);

        assertNotNull(result);
        assertEquals(RIDER_ONLINE, result.message());

        verify(riderRepository, times(1)).save(rider);
    }

    @Test
    void testGoOffline(){
        SecurityUser securityUser = newSecurityUser;
        Rider rider = newRider;

        when(riderRepository.findByExternalId(securityUser.externalId())).thenReturn(Optional.of(rider));
        when(riderRepository.save(rider)).thenReturn(rider);

        var result = riderService.goOffline(securityUser);

        assertNotNull(result);
        assertEquals(RIDER_OFFLINE, result.message());

        verify(riderRepository, times(1)).save(rider);
    }

    @Test
    void testGetAvailableRiders(){
        Pageable pageable = PageRequest.of(0,10);

        Rider rider1 = newRider;
        Rider rider2 = Rider.builder()
                .id(2L)
                .email("rider2@gmail.com")
                .externalId("externalId2")
                .status(Status.ONLINE)
                .availability(AVAILABLE)
                .build();

        List<Rider> riders = List.of(rider1, rider2);
        Page<Rider> riderPage = new PageImpl<>(riders, pageable, riders.size());

        RiderResponse riderResponse1 = RiderResponse.builder()
                .firstName("man")
                .email("rider1@gmail.com")
                .build();

        RiderResponse riderResponse2 = RiderResponse.builder()
                .firstName("woman")
                .email("rider2@gmail.com")
                .build();

        when(riderRepository.findOnlineAndAvailableRiders(pageable)).thenReturn(riderPage);
        when(riderMapper.toRiderResponse(rider1)).thenReturn(riderResponse1);
        when(riderMapper.toRiderResponse(rider2)).thenReturn(riderResponse2);

        Page<RiderResponse> result = riderService.getAvailableRiders(pageable);
        assertThat(result).isNotNull();
        assertEquals(result.getContent().size(),2);
        assertThat(result.getContent().get(0)).isEqualTo(riderResponse1);
        assertThat(result.getContent().get(1)).isEqualTo(riderResponse2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getPageable()).isEqualTo(pageable);

        verify(riderRepository, times(1)).findOnlineAndAvailableRiders(pageable);
        verify(riderMapper, times(1)).toRiderResponse(rider1);
        verify(riderMapper, times(1)).toRiderResponse(rider2);
    }

    @Test
    void testOnlineGetRiderDeliveryRequests(){
        Pageable pageable = PageRequest.of(0,10);
        SecurityUser securityUser = newSecurityUser;
        Rider rider = newRider;
        rider.setAvailability(AVAILABLE);
        rider.setStatus(Status.ONLINE);

        Delivery delivery1 = Delivery.builder()
                .orderId(1L)
                .restaurantId(1L)
                .riderId(1L)
                .customerId(1L)
                .build();

        Delivery delivery2 = Delivery.builder()
                .orderId(2L)
                .restaurantId(2L)
                .riderId(2L)
                .customerId(2L)
                .build();

        List<Delivery> deliveryList = List.of(delivery1, delivery2);
        Page<Delivery> deliveries = new PageImpl<>(deliveryList, pageable, deliveryList.size());

        when(riderRepository.findByExternalId(securityUser.externalId())).thenReturn(Optional.of(rider));
        when(deliveryRepository.findByRiderIdAndStatus(rider.getId(), pageable)).thenReturn(deliveries);

        Page<Delivery> result = riderService.getMyDeliveryRequests(securityUser,pageable);

        assertNotNull(result);
        assertEquals(deliveries.getContent().size(),result.getContent().size());
        assertThat(result.getContent().get(0)).isEqualTo(delivery1);
    }

    @Test
    void testGetRiderDeliveryRequestsForUnavailableRider(){
        Pageable pageable = PageRequest.of(0,10);
        SecurityUser securityUser = newSecurityUser;
        Rider rider = newRider;
        rider.setStatus(Status.ONLINE);

        when(riderRepository.findByExternalId(securityUser.externalId())).thenReturn(Optional.of(rider));

        assertThrows(UnauthorizedException.class, () -> {
            riderService.getMyDeliveryRequests(securityUser,pageable);
        });

        verify(deliveryRepository, never()).findByRiderIdAndStatus(any(), any());
    }

    private RiderRegistrationQueue createRiderObject = RiderRegistrationQueue.builder()
            .accountId(1L)
            .email("testuser@gmail.com")
            .externalId("randomExternalId")
            .build();

    private DeliveryRequestDTO createDeliveryObject = DeliveryRequestDTO.builder()
            .orderId(1L)
            .restaurantId(1L)
            .customerId(1L)
            .build();

    private Rider newRider = Rider.builder()
            .id(1L)
            .accountId(1L)
            .email("testuser@gmail.com")
            .externalId("externalId")
            .firstName("John")
            .lastName("Doe")
            .status(Status.ONLINE)
            .build();

    private SecurityUser newSecurityUser = new SecurityUser(
            "testemail@gmail.com",
            "externalId",
            List.of());

}