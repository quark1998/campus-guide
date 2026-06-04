package com.example.campusguidebackend;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Edge {

    @JsonIgnore
    public Node source;
    @JsonIgnore
    public Node target;

    public double weight;


    public Edge(Node source, Node target, double weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }
}
