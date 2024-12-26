package com.irctc2.route.dto;

import com.irctc2.route.dto.RouteStationDTO;
import lombok.Data;

import java.util.List;

@Data
public class RouteDTO {
    private String trainNumber;
    private String trainName;
    private List<RouteStationDTO> stations;
}
