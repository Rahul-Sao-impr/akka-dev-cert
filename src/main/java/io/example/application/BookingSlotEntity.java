package io.example.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import io.example.domain.BookingEvent;
import io.example.domain.Participant;
import io.example.domain.Timeslot;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("booking-slot")
public class BookingSlotEntity extends EventSourcedEntity<Timeslot, BookingEvent> {

    private final String entityId;
    private static final Logger logger = LoggerFactory.getLogger(BookingSlotEntity.class);

    public BookingSlotEntity(EventSourcedEntityContext context) {
        this.entityId = context.entityId();
    }

    public Effect<Done> markSlotAvailable(Command.MarkSlotAvailable cmd) {
        logger.info("Received command {}, of type {}", cmd, cmd.getClass().getName());
        return effects()
                .persist(new BookingEvent.ParticipantMarkedAvailable(
                        this.entityId,
                        cmd.participant().id(),
                        cmd.participant().participantType()
                ))
                .thenReply(newState -> Done.getInstance());
    }

    public Effect<Done> unmarkSlotAvailable(Command.UnmarkSlotAvailable cmd) {
        logger.info("Received command {}, of type {}", cmd, cmd.getClass().getName());
        return effects()
                .persist(new BookingEvent.ParticipantUnmarkedAvailable(
                        this.entityId,
                        cmd.participant().id(),
                        cmd.participant().participantType()
                ))
                .thenReply(newState -> Done.getInstance());
    }

    // NOTE: booking a slot should produce 3
    // `ParticipantBooked` events
    public Effect<Done> bookSlot(Command.BookReservation cmd) {
        logger.info("Received command {}, of type {}", cmd, cmd.getClass().getName());
        if (!currentState().isBookable(cmd.studentId, cmd.aircraftId, cmd.instructorId)) {
            logger.error("Timeslot is not bookable, at least one of the participants is not available, command: {}", cmd);
            return effects().error("Timeslot is not bookable, at least one of the participants is not available");
        }
        var slotId = this.entityId;
        var sParticipantBookedEvent = new BookingEvent.ParticipantBooked(
                slotId,
                cmd.studentId,
                Participant.ParticipantType.STUDENT,
                cmd.bookingId
        );
        var iParticipantBookedEvent = new BookingEvent.ParticipantBooked(
                slotId,
                cmd.instructorId,
                Participant.ParticipantType.INSTRUCTOR,
                cmd.bookingId
        );
        var aParticipantBookedEvent = new BookingEvent.ParticipantBooked(
                slotId,
                cmd.aircraftId,
                Participant.ParticipantType.AIRCRAFT,
                cmd.bookingId
        );

        return effects()
                .persist(sParticipantBookedEvent, iParticipantBookedEvent, aParticipantBookedEvent)
                .thenReply(newTimeslot -> Done.getInstance());
    }

    // NOTE: canceling a booking should produce 3
    // `ParticipantCanceled` events
    public Effect<Done> cancelBooking(String bookingId) {
        logger.info("Received command to cancel booking, with bookingId: {}", bookingId);
        var slotId = this.entityId;
        var bookings = currentState().findBooking(bookingId);

        if (bookings.isEmpty()) {
            logger.info("No bookings were found with bookingId: {}", bookingId);
            return effects().error("No bookings were available for the booking id provided");
        }

        var events = bookings.stream().map(booking ->
                new BookingEvent.ParticipantCanceled(
                        slotId,
                        booking.participant().id(),
                        booking.participant().participantType(),
                        bookingId
                )
        ).toList();
        return effects()
                .persistAll(events)
                .thenReply(newState -> Done.getInstance());

    }

    public ReadOnlyEffect<Timeslot> getSlot() {
        return effects().reply(currentState());
    }

    @Override
    public Timeslot emptyState() {
        return new Timeslot(
                // NOTE: these are just estimates for capacity based on it being a sample
                HashSet.newHashSet(10), HashSet.newHashSet(10));
    }

    @Override
    public Timeslot applyEvent(BookingEvent event) {
        // Supply your own implementation to update state based
        // on the event
        return switch (event) {
            case BookingEvent.ParticipantMarkedAvailable participantMarkedAvailable ->
                    currentState().reserve(participantMarkedAvailable);
            case BookingEvent.ParticipantBooked participantBooked ->
                    currentState().book(participantBooked);
            case BookingEvent.ParticipantCanceled participantCanceled ->
                    currentState().cancelBooking(participantCanceled.bookingId());
            case BookingEvent.ParticipantUnmarkedAvailable participantUnmarkedAvailable ->
                    currentState().unreserve(participantUnmarkedAvailable);
        };
    }

    public sealed interface Command {
        record MarkSlotAvailable(Participant participant) implements Command {
        }

        record UnmarkSlotAvailable(Participant participant) implements Command {
        }

        record BookReservation(
                String studentId, String aircraftId, String instructorId, String bookingId)
                implements Command {
        }
    }
}
