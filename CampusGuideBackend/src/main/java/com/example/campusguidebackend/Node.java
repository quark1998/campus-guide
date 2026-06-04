package com.example.campusguidebackend;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class Node {
    public Long id;
    public double lat;
    public double lng;
    @JsonIgnore
    public List<Edge> edges = new ArrayList<>();
    @JsonIgnore
    public double minDistance = Double.POSITIVE_INFINITY;

    @JsonIgnore
    public Node previous;

    public Node(Long id, double lat, double lng) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
    }
}
