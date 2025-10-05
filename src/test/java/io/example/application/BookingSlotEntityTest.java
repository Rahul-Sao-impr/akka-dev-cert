package io.example.application;

import akka.javasdk.testkit.EventSourcedTestKit;
import io.example.domain.Participant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BookingSlotEntityTest {

    @Test
    void testInitialState() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);

        Assertions.assertNotNull(testKit.getState());
        Assertions.assertEquals(0, testKit.getState().bookings().size());
        Assertions.assertEquals(0, testKit.getState().available().size());
    }

    @Test
    void testMarkAvailableState() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);

        var request = new BookingSlotEntity.Command.MarkSlotAvailable(
                new Participant("STUD001", Participant.ParticipantType.STUDENT)
        );
        testKit.method(BookingSlotEntity::markSlotAvailable).invoke(request);

        Assertions.assertEquals(0, testKit.getState().bookings().size());
        Assertions.assertEquals(1, testKit.getState().available().size());
    }

    @Test
    void testUnmarkAvailableState() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);

        var markRequest = new BookingSlotEntity.Command.MarkSlotAvailable(
                new Participant("STUD002", Participant.ParticipantType.STUDENT)
        );
        testKit.method(BookingSlotEntity::markSlotAvailable).invoke(markRequest);

        Assertions.assertEquals(1, testKit.getState().available().size());

        var unmarkRequest = new BookingSlotEntity.Command.UnmarkSlotAvailable(
                new Participant("STUD002", Participant.ParticipantType.STUDENT)
        );

        testKit.method(BookingSlotEntity::unmarkSlotAvailable).invoke(unmarkRequest);
        Assertions.assertEquals(0, testKit.getState().available().size());
    }

    @Test
    void testBookSlotParticipantNotMarked() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);
        var studentId = "STUD001";
        var aircraftId = "AIRC001";
        var instructorId = "INST001";
        var bookingId = "BOOK001";


        var bookSlotRequest = new BookingSlotEntity.Command.BookReservation(
                studentId,
                aircraftId,
                instructorId,
                bookingId
        );

        testKit.method(BookingSlotEntity::bookSlot).invoke(bookSlotRequest);
        Assertions.assertEquals(0, testKit.getState().bookings().size());
    }

    @Test
    void testBookSlotParticipantMarkedAvailable() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);
        var studentId = "STUD001";
        var aircraftId = "AIRC001";
        var instructorId = "INST001";
        var bookingId = "BOOK001";

        var student = new Participant(studentId, Participant.ParticipantType.STUDENT);
        var aircraft = new Participant(aircraftId, Participant.ParticipantType.AIRCRAFT);
        var instructor = new Participant(instructorId, Participant.ParticipantType.INSTRUCTOR);

        testKit.method(BookingSlotEntity::markSlotAvailable).invoke(new BookingSlotEntity.Command.MarkSlotAvailable(student));
        testKit.method(BookingSlotEntity::markSlotAvailable).invoke(new BookingSlotEntity.Command.MarkSlotAvailable(aircraft));
        testKit.method(BookingSlotEntity::markSlotAvailable).invoke(new BookingSlotEntity.Command.MarkSlotAvailable(instructor));

        var bookSlotRequest = new BookingSlotEntity.Command.BookReservation(
                studentId,
                aircraftId,
                instructorId,
                bookingId
        );

        testKit.method(BookingSlotEntity::bookSlot).invoke(bookSlotRequest);
        Assertions.assertEquals(3, testKit.getState().bookings().size());
    }

    @Test
    void testCancelBooking() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);
        var studentId = "STUD001";
        var aircraftId = "AIRC001";
        var instructorId = "INST001";
        var bookingId = "BOOK001";

        var student = new Participant(studentId, Participant.ParticipantType.STUDENT);
        var aircraft = new Participant(aircraftId, Participant.ParticipantType.AIRCRAFT);
        var instructor = new Participant(instructorId, Participant.ParticipantType.INSTRUCTOR);

        testKit.method(BookingSlotEntity::markSlotAvailable).invoke(new BookingSlotEntity.Command.MarkSlotAvailable(student));
        testKit.method(BookingSlotEntity::markSlotAvailable).invoke(new BookingSlotEntity.Command.MarkSlotAvailable(aircraft));
        testKit.method(BookingSlotEntity::markSlotAvailable).invoke(new BookingSlotEntity.Command.MarkSlotAvailable(instructor));

        var bookSlotRequest = new BookingSlotEntity.Command.BookReservation(
                studentId,
                aircraftId,
                instructorId,
                bookingId
        );

        Assertions.assertEquals(0, testKit.getState().bookings().size());
        testKit.method(BookingSlotEntity::bookSlot).invoke(bookSlotRequest);
        Assertions.assertEquals(3, testKit.getState().bookings().size());

        testKit.method(BookingSlotEntity::cancelBooking).invoke(bookingId);
        Assertions.assertEquals(0, testKit.getState().bookings().size());
    }

    @Test
    void testCancelNonExistingBooking() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);

        var response = testKit.method(BookingSlotEntity::cancelBooking).invoke("test001");
        Assertions.assertEquals("No bookings were available for the booking id provided", response.getError());
    }
}
