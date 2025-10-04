package io.example.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import io.example.domain.BookingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is responsible for consuming events from the booking
// slot entity and turning those into command calls on the
// participant slot entity
@ComponentId("blooking-slot-consumer")
@Consume.FromEventSourcedEntity(BookingSlotEntity.class)
public class SlotToParticipantConsumer extends Consumer {

    private final ComponentClient client;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public SlotToParticipantConsumer(ComponentClient client) {
        this.client = client;
    }

    public Effect onEvent(BookingEvent event) {
        var participantSlotEntityId = participantSlotId(event);
        logger.info("Received BookingEvent : {}... generated participantSlotEntityId {}", event, participantSlotEntityId);
        switch (event) {
            case BookingEvent.ParticipantCanceled participantCanceled ->
                    this.client
                            .forEventSourcedEntity(participantSlotEntityId)
                            .method(ParticipantSlotEntity::cancel)
                            .invoke(new ParticipantSlotEntity.Commands.Cancel(
                                    participantSlotEntityId,
                                    participantCanceled.participantId(),
                                    participantCanceled.participantType(),
                                    participantCanceled.bookingId()
                            ));
            case BookingEvent.ParticipantBooked participantBooked ->
                    this.client
                            .forEventSourcedEntity(participantSlotEntityId)
                            .method(ParticipantSlotEntity::book)
                            .invoke(new ParticipantSlotEntity.Commands.Book(
                                    participantSlotEntityId,
                                    participantBooked.participantId(),
                                    participantBooked.participantType(),
                                    participantBooked.bookingId()
                            ));
            case BookingEvent.ParticipantMarkedAvailable participantMarkedAvailable ->
                    this.client
                            .forEventSourcedEntity(participantSlotEntityId)
                            .method(ParticipantSlotEntity::markAvailable)
                            .invoke(new ParticipantSlotEntity.Commands.MarkAvailable(
                                    participantSlotEntityId,
                                    participantMarkedAvailable.participantId(),
                                    participantMarkedAvailable.participantType()
                            ));
            case BookingEvent.ParticipantUnmarkedAvailable participantUnmarkedAvailable ->
                    this.client
                            .forEventSourcedEntity(participantSlotEntityId)
                            .method(ParticipantSlotEntity::unmarkAvailable)
                            .invoke(new ParticipantSlotEntity.Commands.UnmarkAvailable(
                                    participantSlotEntityId,
                                    participantUnmarkedAvailable.participantId(),
                                    participantUnmarkedAvailable.participantType()
                            ));
        }
        return effects().done();
    }

    // Participant slots are keyed by a derived key made up of
    // {slotId}-{participantId}
    // We don't need the participant type here because the participant IDs
    // should always be unique/UUIDs
    private String participantSlotId(BookingEvent event) {
        return switch (event) {
            case BookingEvent.ParticipantBooked evt -> evt.slotId() + "-" + evt.participantId();
            case BookingEvent.ParticipantUnmarkedAvailable evt ->
                evt.slotId() + "-" + evt.participantId();
            case BookingEvent.ParticipantMarkedAvailable evt -> evt.slotId() + "-" + evt.participantId();
            case BookingEvent.ParticipantCanceled evt -> evt.slotId() + "-" + evt.participantId();
        };
    }
}
