package io.example.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import io.example.application.ParticipantSlotEntity.Event.Booked;
import io.example.application.ParticipantSlotEntity.Event.Canceled;
import io.example.application.ParticipantSlotEntity.Event.MarkedAvailable;
import io.example.application.ParticipantSlotEntity.Event.UnmarkedAvailable;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("view-participant-slots")
public class ParticipantSlotsView extends View {

    private static Logger logger = LoggerFactory.getLogger(ParticipantSlotsView.class);

    @Consume.FromEventSourcedEntity(ParticipantSlotEntity.class)
    public static class ParticipantSlotsViewUpdater extends TableUpdater<SlotRow> {

        public Effect<SlotRow> onEvent(ParticipantSlotEntity.Event event) {
            return switch (event) {
                case Booked booked -> effects().updateRow(
                        new SlotRow(
                                booked.slotId(),
                                booked.participantId(),
                                booked.participantType().name(),
                                booked.bookingId(),
                                "BOOKED"
                        )
                );
                case Canceled canceled -> effects().updateRow(
                        new SlotRow(
                                canceled.slotId(),
                                canceled.participantId(),
                                canceled.participantType().name(),
                                canceled.bookingId(),
                                "CANCELED"
                        )
                );
                case MarkedAvailable markedAvailable -> effects().updateRow(
                        new SlotRow(
                                markedAvailable.slotId(),
                                markedAvailable.participantId(),
                                markedAvailable.participantType().name(),
                                "NOT_BOOKED",
                                "AVAILABLE"
                        )
                );
                case UnmarkedAvailable unmarkedAvailable -> effects().updateRow(
                        new SlotRow(
                                unmarkedAvailable.slotId(),
                                unmarkedAvailable.participantId(),
                                unmarkedAvailable.participantType().name(),
                                "NOT_BOOKED",
                                "UNAVAILABLE"
                        )
                );
            };
        }
    }

    public record SlotRow(
            String slotId,
            String participantId,
            String participantType,
            String bookingId,
            String status) {
    }

    public record ParticipantStatusInput(String participantId, String status) {
    }

    public record SlotList(List<SlotRow> slots) {
    }

    @Query("SELECT * as slots FROM participant_slots_view WHERE participantId = :participantId")
    public QueryEffect<SlotList> getSlotsByParticipant(String participantId) {
        return queryResult();
    }

    @Query(
       """
       SELECT * as slots FROM participant_slots_view
       WHERE participantId = :participantId AND status = :status"""
    )
    public QueryEffect<SlotList> getSlotsByParticipantAndStatus(ParticipantStatusInput input) {
        return queryResult();
    }
}
