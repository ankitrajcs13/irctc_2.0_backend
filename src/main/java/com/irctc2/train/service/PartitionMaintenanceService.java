package com.irctc2.train.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;



@Service
public class PartitionMaintenanceService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private final JdbcTemplate jdbcTemplate;
    private static final LocalDate ANCHOR_DATE = LocalDate.of(2025, 2, 11);
    public PartitionMaintenanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    @Autowired
    private DiscordNotificationService discordNotificationService;
    @Autowired
    private MaintenanceModeService maintenanceModeService;
    /**
     * Run this method every day at 2:00 AM.
     */
    @Scheduled(cron = "0 20 0 * * ?", zone = "Asia/Kolkata")
    public void maintainPartitions() {
        maintenanceModeService.setMaintenance(true);
        long startTime = System.currentTimeMillis();
        try {
            LocalDate today = LocalDate.now();
            LocalDate windowEnd = today.plusDays(60);
            LocalDate currentDate = LocalDate.now(); // For current date only
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Desired date format
            String formattedDate = currentDate.format(formatter);
            // Create message including the current date
            String message = "Cron job for partitioning and seeding data started successfully on " + formattedDate + ".";

            discordNotificationService.sendDiscordMessage(message);

            // Compute the next aligned partition strt date based on the anchor date.
            long daysSinceAnchor = ChronoUnit.DAYS.between(ANCHOR_DATE, today);
            long remainder = daysSinceAnchor % 10;
            LocalDate alignedStart = remainder == 0 ? today : today.plusDays(10 - remainder);
            // TODO DON"T USE THIS -> USE alignedStart
            LocalDate cycleStart = alignedStart;

            // 1. Create partitions for any new 10-day ranges in the 60-day window, starting from the aligned start date.
            for (LocalDate start = cycleStart; start.isBefore(windowEnd); start = start.plusDays(10)) {
                LocalDate end = start.plusDays(10);
                String partitionName = "seat_availability_" + start.format(FORMATTER) + "_" + end.format(FORMATTER);
                if (!partitionExists(partitionName)) {
                    String createSql = "CREATE TABLE " + partitionName + " PARTITION OF seat_availability " +
                            "FOR VALUES FROM ('" + start.toString() + "') TO ('" + end.toString() + "')";
                    jdbcTemplate.execute(createSql);
                    System.out.println("Created partition: " + partitionName);
                }
                seedPartition(start, end);
            }

            // 2. Drop partitions that are no longer needed (i.e. partitions ending before today)
            List<String> partitions = jdbcTemplate.queryForList(
                    "SELECT relname FROM pg_class WHERE relname LIKE 'seat_availability\\_%' ESCAPE '\\'",
                    String.class
            );

            for (String partitionName : partitions) {
                // Partition name format: seat_availability_YYYY_MM_DD_YYYY_MM_DD
                // Remove the prefix to get the date range part.
                String suffix = partitionName.substring("seat_availability_".length());
                String[] dateParts = suffix.split("_");
                // We expect 6 parts: [YYYY, MM, DD, YYYY, MM, DD]
                if (dateParts.length != 6) {
                    continue;
                }
                // Construct the partition's upper bound date from the last three parts.
                String endDateStr = dateParts[3] + "-" + dateParts[4] + "-" + dateParts[5];
                LocalDate partitionEnd = LocalDate.parse(endDateStr);
                // If the partition’s end date is before today, it’s out of the window.
                if (partitionEnd.isBefore(today)) {
                    String dropSql = "DROP TABLE IF EXISTS " + partitionName;
                    jdbcTemplate.execute(dropSql);
                    System.out.println("Dropped partition: " + partitionName);
                }
            }
            long duration = System.currentTimeMillis() - startTime;
            discordNotificationService.sendDiscordMessage("✅ Partitioning & seeding completed in " + duration / 1000 + " seconds.");
        }  finally {
            maintenanceModeService.setMaintenance(false); // 🔓 Always reset
        }
    }

    /**
     * Check if a partition with the given name exists.
     */
    private boolean partitionExists(String partitionName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_class WHERE relname = ?",
                new Object[]{partitionName},
                Integer.class
        );
        return count != null && count > 0;
    }
    // TODO WILL CHECK SCHEDULE TO HANDLE IN TRAIN
    // TODO FIX CAPACITY IS NULL
    private void seedPartition(LocalDate start, LocalDate end) {
        // Iterate over each date in the partition.
        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
            // Determine the day of week in lower-case (e.g., "monday").
            String dayOfWeek = date.getDayOfWeek().toString().toLowerCase();

            // Query trains running on this day.
            String trainQuery = "SELECT t.id AS train_id, t.train_number " +
                    "FROM trains t " +
                    "JOIN train_running_days trd ON t.id = trd.train_id " +
                    "WHERE LOWER(trd.day) = ?";
            List<Map<String, Object>> trains = jdbcTemplate.queryForList(trainQuery, dayOfWeek);

            // For each train, query its bogies and insert seat records.
            for (Map<String, Object> trainRow : trains) {
                Long trainId = (Long) trainRow.get("train_id");

                // ✅ Skip if this train has already been seeded for this date
                String checkSql = "SELECT COUNT(*) FROM seat_availability WHERE train_id = ? AND travel_date = ?";
                Integer count = jdbcTemplate.queryForObject(checkSql, new Object[]{trainId, date}, Integer.class);
                if (count != null && count > 0) {
                    System.out.println("Skipping train_id " + trainId + " on " + date + " (already seeded)");
                    discordNotificationService.sendDiscordMessage("✅ Skipping Train with " + trainId);
                    continue;
                }


                // Query bogies for the current train.
                String bogieQuery = "SELECT id AS bogie_id, bogie_name, bogie_type " +
                        "FROM bogies WHERE train_id = ?";
                List<Map<String, Object>> bogies = jdbcTemplate.queryForList(bogieQuery, trainId);

                // For each bogie, insert a record for each seat (from 1 up to capacity).
                for (Map<String, Object> bogieRow : bogies) {
                    Long bogieId = (Long) bogieRow.get("bogie_id");
                    String bogieName = (String) bogieRow.get("bogie_name");
                    String bogieType = (String) bogieRow.get("bogie_type");

                    // Skip processing for General bogies.
                    if (bogieType.substring(0, 2).equals("GE")) {
                        continue;
                    }

                    // Determine the seat count using the first two characters of bogieType.
                    int seatCount;
                    switch (bogieType.substring(0, 2)) {
                        case "SL": // Sleeper
                        case "TH": // Third AC
                            seatCount = 72;
                            break;
                        case "SE": // Second AC
                            seatCount = 54;
                            break;
                        case "FI": // First AC
                            seatCount = 18;
                            break;
                        case "GE": // General
                            seatCount = 90;
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid bogie name prefix: " + bogieType);
                    }

                    for (int seatNumber = 1; seatNumber <= seatCount; seatNumber++) {
                        String insertSql = "INSERT INTO seat_availability " +
                                "(train_id, travel_date, bogie_id, seat_number, bogie_type, bogie_name, booked_segments, version) " +
                                "VALUES (?, ?, ?, ?, ?, ?, 0, 1)";
                        jdbcTemplate.update(insertSql,
                                trainId,
                                date,
                                bogieId,
                                seatNumber,
                                bogieType,
                                bogieName);
                    }
                }
            }
            System.out.println("Seeded seat availability for date: " + date);
        }
    }

}