package com.irctc2.route.controller;

import com.irctc2.route.dto.RouteDTO;
import com.irctc2.route.model.Route;
import com.irctc2.route.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @PostMapping("/{trainId}")
    public Route createRoute(@PathVariable String trainId, @RequestBody Route route) {
        return routeService.createRoute(trainId, route);
    }

//    @GetMapping
//    public List<RouteDTO> getAllRoutes() {
//        return routeService.getAllRoutes();
//    }

    @GetMapping("/search")
    public Map<String, Object> findTrainsBetweenStations(
            @RequestParam String sourceStation,
            @RequestParam String destinationStation,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate
    ) {
        List<RouteDTO> trains = routeService.findTrainsBetweenStations(sourceStation, destinationStation, travelDate);

        // Create a response map
        Map<String, Object> response = new HashMap<>();
        response.put("count", trains.size());
        response.put("trains", trains);

        return response;
    }
}
