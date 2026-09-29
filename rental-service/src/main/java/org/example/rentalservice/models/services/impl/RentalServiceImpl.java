package org.example.rentalservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.rentalservice.models.constants.RentalStatus;
import org.example.rentalservice.models.dto.requests.CreateRentalDetailRequest;
import org.example.rentalservice.models.dto.requests.CreateRentalRequest;
import org.example.rentalservice.models.dto.responses.RentalDetailResponse;
import org.example.rentalservice.models.dto.responses.RentalResponse;
import org.example.rentalservice.models.dto.responses.VehicleResponse;
import org.example.rentalservice.models.entities.Rental;
import org.example.rentalservice.models.entities.RentalDetail;
import org.example.rentalservice.models.repositories.RentalDetailRepository;
import org.example.rentalservice.models.repositories.RentalRepository;
import org.example.rentalservice.models.services.RentalService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {

    private static final String RENTAL_CREATED_TOPIC = "rental-created";

    private final RentalRepository rentalRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final VehicleGatewayService vehicleGatewayService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public RentalResponse createRental(CreateRentalRequest request) {
        final String customerName = request.customerName().trim();
        final String customerEmail = request.customerEmail().trim();
        final List<RentalDetail> rentalDetails = new ArrayList<>();
        final List<RentalDetailResponse> detailResponses = new ArrayList<>();
        double totalAmount = 0.0;

        for (CreateRentalDetailRequest item : request.items()) {
            VehicleResponse vehicle = vehicleGatewayService.getVehicleById(item.vehicleId());
            if (vehicle == null || vehicle.vehicleId() == null || vehicle.vehicleName() == null || vehicle.vehicleType() == null || vehicle.pricePerDay() == null) {
                throw new IllegalStateException("Vehicle service returned invalid vehicle data for id: " + item.vehicleId());
            }

            double pricePerDay = vehicle.pricePerDay();
            int rentalDays = item.rentalDays();
            double lineTotal = pricePerDay * rentalDays;
            totalAmount += lineTotal;

            rentalDetails.add(RentalDetail.builder()
                    .vehicleId(vehicle.vehicleId())
                    .rentalDays(rentalDays)
                    .pricePerDay(pricePerDay)
                    .build());

            detailResponses.add(new RentalDetailResponse(
                    null,
                    vehicle.vehicleId(),
                    vehicle.vehicleName(),
                    vehicle.vehicleType(),
                    rentalDays,
                    pricePerDay,
                    lineTotal
            ));
        }

        Rental rental = Rental.builder()
                .customerName(customerName)
                .customerEmail(customerEmail)
                .totalAmount(totalAmount)
                .status(RentalStatus.PENDING)
                .build();

        rental = rentalRepository.save(rental);

        for (int i = 0; i < rentalDetails.size(); i++) {
            RentalDetail detail = rentalDetails.get(i);
            detail.setRental(rental);
            detailResponses.set(i, new RentalDetailResponse(
                    null,
                    detail.getVehicleId(),
                    detailResponses.get(i).vehicleName(),
                    detailResponses.get(i).vehicleType(),
                    detail.getRentalDays(),
                    detail.getPricePerDay(),
                    detailResponses.get(i).lineTotal()
            ));
        }

        List<RentalDetail> savedRentalDetails = rentalDetailRepository.saveAll(rentalDetails);
        List<RentalDetailResponse> finalResponses = new ArrayList<>();
        for (int i = 0; i < savedRentalDetails.size(); i++) {
            RentalDetail saved = savedRentalDetails.get(i);
            RentalDetailResponse response = detailResponses.get(i);
            finalResponses.add(new RentalDetailResponse(
                    saved.getId(),
                    saved.getVehicleId(),
                    response.vehicleName(),
                    response.vehicleType(),
                    saved.getRentalDays(),
                    saved.getPricePerDay(),
                    response.lineTotal()
            ));
        }

        rental.setTotalAmount(totalAmount);
        rental = rentalRepository.save(rental);

        kafkaTemplate.send(RENTAL_CREATED_TOPIC, customerEmail);

        return new RentalResponse(
                rental.getId(),
                rental.getCustomerName(),
                rental.getCustomerEmail(),
                rental.getTotalAmount(),
                rental.getStatus(),
                finalResponses
        );
    }
}
