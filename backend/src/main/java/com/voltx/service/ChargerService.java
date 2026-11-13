package com.voltx.service;

import com.voltx.dto.request.ChargerRequest;
import com.voltx.dto.request.ChargerStatusRequest;
import com.voltx.dto.request.ChargerUpdateRequest;
import com.voltx.dto.response.BookedSlotResponse;
import com.voltx.dto.response.ChargerResponse;

import java.time.LocalDate;
import java.util.List;

public interface ChargerService {

    ChargerResponse get(Long chargerId);

    List<BookedSlotResponse> bookedSlots(Long chargerId, LocalDate date);

    List<ChargerResponse> listAll(Long stationId);

    ChargerResponse create(String adminEmail, ChargerRequest request);

    ChargerResponse update(String adminEmail, Long chargerId, ChargerUpdateRequest request);

    ChargerResponse changeStatus(String adminEmail, Long chargerId, ChargerStatusRequest request);
}
