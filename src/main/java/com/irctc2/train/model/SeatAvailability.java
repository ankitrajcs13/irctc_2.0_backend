package com.irctc2.train.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "seat_availability",
        uniqueConstraints = @UniqueConstraint(columnNames = {"train_id", "travel_date", "bogie_id", "seat_number"}),
        indexes = @Index(name = "idx_train_date_bogie_seat", columnList = "train_id, travel_date, bogie_id, seat_number")
)
public class SeatAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to the train (could be a foreign key to Train.id)
    @Column(name = "train_id", nullable = false)
    private Long trainId;

    // The travel date for which the seat status is maintained.
    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    // Reference to the bogie (foreign key to Bogie.id)
    @Column(name = "bogie_id", nullable = false, insertable = false, updatable = false)
    private Long bogieId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bogie_id", nullable = false)
    private Bogie bogie;

    // The seat number inside the bogie.
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    // Additional fields for filtering and display.
    @Column(name = "bogie_type", nullable = false)
    private String bogieType;

    @Column(name = "bogie_name", nullable = false)
    private String bogieName;
    /**
     * Instead of a simple "isAvailable" flag, we use a bit mask stored in a Long to track bookings
     * for up to 50 segments. Each bit represents one segment (0 to 49). A bit value of 1 indicates that
     * the segment is already booked.
     */
    // Indicates whether the seat is available.
    @Column(name = "booked_segments", nullable = false)
    private Long bookedSegments = 0L;

    // For optimistic locking; helps prevent concurrent modifications.
    @Version
    private Long version;

    /**
     * Checks if the seat is available for a given segment range.
     *
     * Segments are numbered from 0 to 49. The range is specified with a starting segment index (inclusive)
     * and an ending segment index (exclusive). For example, if you want to check segments 2 through 4,
     * you’d call isSegmentRangeAvailable(2, 5).
     *
     * @param startSegment inclusive start segment index (0-indexed)
     * @param endSegment   exclusive end segment index
     * @return true if none of the segments in the specified range are booked; false otherwise.
     */
    public boolean isSegmentRangeAvailable(int startSegment, int endSegment) {
        long mask = createMask(startSegment, endSegment);
        // If any bit in the mask is already set, the segment is not available.
        return (bookedSegments & mask) == 0;
    }

    /**
     * Books the given segment range.
     *
     * This method should only be called if the segments are available (i.e. after a successful call
     * to isSegmentRangeAvailable). If any of the segments are already booked, an IllegalStateException
     * is thrown.
     *
     * @param startSegment inclusive start segment index (0-indexed)
     * @param endSegment   exclusive end segment index
     */
    public void bookSegments(int startSegment, int endSegment) {
        long mask = createMask(startSegment, endSegment);
        if ((bookedSegments & mask) != 0) {
            throw new IllegalStateException("Some segments are already booked.");
        }
        bookedSegments |= mask;
    }

    /**
     * Helper method to create a bit mask for segments from startSegment (inclusive) to endSegment (exclusive).
     *
     * For example, if startSegment is 2 and endSegment is 5, then the mask will have bits 2, 3, and 4 set.
     *
     * @param startSegment inclusive start segment index
     * @param endSegment   exclusive end segment index
     * @return a bit mask with bits set for the specified segment range
     * @throws IllegalArgumentException if the segment range is invalid (e.g. negative, beyond 50, or start >= end)
     */
    private long createMask(int startSegment, int endSegment) {
        if (startSegment < 0 || endSegment > 50 || startSegment >= endSegment) {
            throw new IllegalArgumentException("Invalid segment range. Allowed segments: 0 to 50 with start < end.");
        }
        // For a range of (endSegment - startSegment) segments, create a mask with that many 1's,
        // then shift it left by startSegment.
        return ((1L << (endSegment - startSegment)) - 1) << startSegment;
    }
}
