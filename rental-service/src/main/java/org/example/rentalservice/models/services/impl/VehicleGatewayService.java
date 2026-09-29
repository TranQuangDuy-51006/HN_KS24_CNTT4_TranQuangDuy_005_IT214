package org.example.rentalservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.rentalservice.clients.VehicleClient;
import org.example.rentalservice.exceptions.VehicleNotFoundException;
import org.example.rentalservice.exceptions.VehicleServiceException;
import org.example.rentalservice.models.dto.responses.VehicleResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VehicleGatewayService {

    private final VehicleClient vehicleClient;

    @CircuitBreaker(name = "vehicleService", fallbackMethod = "getVehicleByIdFallback")
    public VehicleResponse getVehicleById(Long vehicleId) {
        try {
            VehicleResponse vehicle = vehicleClient.getVehicleById(vehicleId);
            if (vehicle == null || vehicle.vehicleId() == null || vehicle.vehicleName() == null || vehicle.vehicleType() == null || vehicle.pricePerDay() == null) {
                throw new VehicleServiceException("Vehicle service returned invalid data for vehicle id: " + vehicleId);
            }
            return vehicle;
        } catch (FeignException.NotFound e) {
            throw new VehicleNotFoundException(vehicleId);
        } catch (FeignException e) {
            throw new VehicleServiceException("Vehicle service unavailable for vehicle id: " + vehicleId, e);
        }
    }

    private VehicleResponse getVehicleByIdFallback(Long vehicleId, Throwable throwable) {
        if (throwable instanceof VehicleNotFoundException notFoundException) {
            throw notFoundException;
        }
        throw new VehicleServiceException("Vehicle service unavailable for vehicle id: " + vehicleId, throwable);
    }
}
