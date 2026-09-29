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
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {

        private final RentalRepository rentalRepository;
        private final RentalDetailRepository rentalDetailRepository;
        private final VehicleGatewayService vehicleGatewayService;

        @Override
        public RentalResponse createRental(CreateRentalRequest request) {
                throw new UnsupportedOperationException();
        }
}
