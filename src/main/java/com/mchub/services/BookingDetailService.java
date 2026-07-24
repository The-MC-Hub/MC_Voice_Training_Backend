package com.mchub.services;

import com.mchub.models.BookingDetail;

public interface BookingDetailService {

    BookingDetail getOrCreate(String bookingId);

    BookingDetail update(String bookingId, BookingDetail updated);
}
