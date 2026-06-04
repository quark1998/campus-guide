package com.example.campusguidebackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class CampusGuideBackendApplication {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        SpringApplication.run(CampusGuideBackendApplication.class, args);
    }

    @PostConstruct
    public void initRoadNetworkTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS map_nodes (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(100) DEFAULT ''," +
                "lat DOUBLE NOT NULL," +
                "lng DOUBLE NOT NULL" +
                ")");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS map_edges (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "source_id BIGINT NOT NULL," +
                "target_id BIGINT NOT NULL," +
                "distance DOUBLE NOT NULL," +
                "INDEX idx_source_id (source_id)," +
                "INDEX idx_target_id (target_id)" +
                ")");
    }
}