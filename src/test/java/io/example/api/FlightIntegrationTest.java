package io.example.api;

import akka.http.javadsl.model.StatusCodes;
import akka.javasdk.http.StrictResponse;
import akka.javasdk.testkit.TestKitSupport;
import akka.util.ByteString;
import io.example.application.ParticipantSlotsView;
import io.example.domain.Timeslot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FlightIntegrationTest extends TestKitSupport {
    final String URL_PREFIX = "/flight";

    @Test
    public void createBookingWhenParticipantsMarkedAvailableOverHttp() {

        var slotId = "SL001";
        var studentId = "STU001";
        var instructorId = "INS001";
        var aircraftId = "AIR001";
        var bookingId = "BK001";

        var sAvailabilityRequest = new FlightEndpoint.AvailabilityRequest(studentId, "STUDENT");
        var aAvailabilityRequest = new FlightEndpoint.AvailabilityRequest(aircraftId, "AIRCRAFT");
        var iAvailabilityRequest = new FlightEndpoint.AvailabilityRequest(instructorId, "INSTRUCTOR");

        // mark available
        markAvailable(slotId, sAvailabilityRequest);
        markAvailable(slotId, aAvailabilityRequest);
        markAvailable(slotId, iAvailabilityRequest);

        var bookingRequest = new FlightEndpoint.BookingRequest(studentId, aircraftId, instructorId, bookingId);
        // create booking
        var createBookingResponse = createBooking(slotId, bookingRequest);

        Assertions.assertEquals(StatusCodes.CREATED, createBookingResponse.status());

        var getSlotResponse = getSlot(slotId);
        Assertions.assertTrue(getSlotResponse.status().isSuccess());
        var bookedSlot = getSlotResponse.body().bookings();
        Assertions.assertEquals(3, getSlotResponse.body().bookings().size());
        Assertions.assertEquals(bookingId,bookedSlot.stream().findFirst().get().bookingId());

        // cancel booking
        var cancelBookingResponse = cancelBooking(slotId, bookingId);
        Assertions.assertEquals(StatusCodes.OK, cancelBookingResponse.status());
    }

    @Test
    public void createBookingWhenParticipantsNotMarkedAvailableOverHttp() {

        var slotId = "SL002";
        var studentId = "STU002";
        var instructorId = "INS002";
        var aircraftId = "AIR002";
        var bookingId = "BK002";

        var sAvailabilityRequest = new FlightEndpoint.AvailabilityRequest(studentId, "STUDENT");
        var aAvailabilityRequest = new FlightEndpoint.AvailabilityRequest(aircraftId, "AIRCRAFT");

        // mark available - only student and aircraft but not the instructor
        markAvailable(slotId, sAvailabilityRequest);
        markAvailable(slotId, aAvailabilityRequest);

        var bookingRequest = new FlightEndpoint.BookingRequest(studentId, aircraftId, instructorId, bookingId);
        // create booking
        var createBookingResponse = createBooking(slotId, bookingRequest);

        Assertions.assertEquals(StatusCodes.BAD_REQUEST, createBookingResponse.status());
    }

    @Test
    public void markedParticipantsAvailableOverHttp() {

        var slotId = "SL003";
        var studentId = "STU003";

        var sAvailabilityRequest = new FlightEndpoint.AvailabilityRequest(studentId, "STUDENT");

        // mark student available
        var markAvailableResponse = markAvailable(slotId, sAvailabilityRequest);

        Assertions.assertEquals(StatusCodes.OK, markAvailableResponse.status());
    }

    @Test
    public void unmarkedParticipantsAvailableOverHttp() {

        var slotId = "SL004";
        var studentId = "STU004";
        var instructorId = "INS004";
        var aircraftId = "AIR004";
        var bookingId = "BK004";

        var sAvailabilityRequest = new FlightEndpoint.AvailabilityRequest(studentId, "STUDENT");
        var aAvailabilityRequest = new FlightEndpoint.AvailabilityRequest(aircraftId, "AIRCRAFT");
        var iAvailabilityRequest = new FlightEndpoint.AvailabilityRequest(instructorId, "INSTRUCTOR");

        // mark available
        markAvailable(slotId, sAvailabilityRequest);
        markAvailable(slotId, aAvailabilityRequest);
        markAvailable(slotId, iAvailabilityRequest);

        var bookingRequest = new FlightEndpoint.BookingRequest(studentId, aircraftId, instructorId, bookingId);
        // create booking
        var createBookingResponse = createBooking(slotId, bookingRequest);

        Assertions.assertEquals(StatusCodes.CREATED, createBookingResponse.status());

        // get slot
        var getSlotResponse = getSlot(slotId);
        Assertions.assertTrue(getSlotResponse.status().isSuccess());
        var bookedSlot = getSlotResponse.body().bookings();
        Assertions.assertEquals(3, getSlotResponse.body().bookings().size());
        Assertions.assertEquals(bookingId,bookedSlot.stream().findFirst().get().bookingId());

        // cancel booking
        var cancelBookingResponse = cancelBooking(slotId, bookingId);
        Assertions.assertEquals(StatusCodes.OK, cancelBookingResponse.status());
        // mark available
        markAvailable(slotId, sAvailabilityRequest);
        markAvailable(slotId, aAvailabilityRequest);
        markAvailable(slotId, iAvailabilityRequest);

        // unmark aircraft
        var unmarkAircraftResponse = unmarkAvailable(slotId, aAvailabilityRequest);
        Assertions.assertEquals(StatusCodes.OK, unmarkAircraftResponse.status());

        var againCreateBookingResponse = createBooking(slotId, bookingRequest);

        Assertions.assertEquals(StatusCodes.BAD_REQUEST, againCreateBookingResponse.status());
    }

    @Test
    public void getSlotListsByStatusOverHttp() throws InterruptedException {

        var slotId = "SL005";
        var studentId = "STU005";
        var aircraftId = "AIR005";

        var sAvailabilityRequest = new FlightEndpoint.AvailabilityRequest(studentId, "STUDENT");
        var aAvailabilityRequest = new FlightEndpoint.AvailabilityRequest(aircraftId, "AIRCRAFT");

        // mark available
        markAvailable(slotId, sAvailabilityRequest);
        markAvailable(slotId, aAvailabilityRequest);
        // unmark available
        unmarkAvailable(slotId, aAvailabilityRequest);

        Thread.sleep(7000l);
        var getStudentSlotByStatusResp = getSlotsByStatus(studentId, "AVAILABLE");
        Assertions.assertEquals(1, getStudentSlotByStatusResp.body().slots().size());

        var getAircraftSlotByStatusResp = getSlotsByStatus(aircraftId, "UNAVAILABLE");
        Assertions.assertEquals(1, getAircraftSlotByStatusResp.body().slots().size());
    }

    @Test
    public void getCancelNonExistingBookingOverHttp() throws InterruptedException {

        var slotId = "SL006";
        var bookingId = "BK006";

        // cancel booking
        var cancelBookingResponse = cancelBooking(slotId, bookingId);
        Assertions.assertEquals(StatusCodes.BAD_REQUEST, cancelBookingResponse.status());
    }

    private StrictResponse<ByteString> markAvailable(String slotId, FlightEndpoint.AvailabilityRequest request) {
        return httpClient.POST(URL_PREFIX + "/availability/" + slotId)
                .withRequestBody(request).invoke();
    }

    private StrictResponse<ByteString> unmarkAvailable(String slotId, FlightEndpoint.AvailabilityRequest request) {
        return httpClient.DELETE(URL_PREFIX + "/availability/" + slotId)
                .withRequestBody(request).invoke();
    }

    private StrictResponse<ByteString> createBooking(String slotId, FlightEndpoint.BookingRequest request) {
        return httpClient.POST(URL_PREFIX + "/bookings/" + slotId)
                .withRequestBody(request).invoke();
    }

    private StrictResponse<ByteString> cancelBooking(String slotId, String bookingId) {
        return httpClient.DELETE(URL_PREFIX + "/bookings/" + slotId + "/" + bookingId).invoke();
    }

    private StrictResponse<Timeslot> getSlot(String slotId) {
        return httpClient.GET(URL_PREFIX + "/availability/" + slotId).responseBodyAs(Timeslot.class).invoke();
    }

    private StrictResponse<ParticipantSlotsView.SlotList> getSlotsByStatus(String participantId, String status) {
        return httpClient.GET(URL_PREFIX + "/slots/" + participantId +"/" + status)
                .responseBodyAs(ParticipantSlotsView.SlotList.class).invoke();
    }
}
