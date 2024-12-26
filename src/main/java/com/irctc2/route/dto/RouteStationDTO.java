package com.irctc2.route.dto;

import lombok.Data;

@Data
public class RouteStationDTO {
    private String stationName;
    private int stationOrder;
    private int day;
    private String arrivalTime;
    private String departureTime;
    private String haltTime;
    private String distance;
}
