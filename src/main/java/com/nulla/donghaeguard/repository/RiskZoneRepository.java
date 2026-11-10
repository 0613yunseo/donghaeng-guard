package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.RiskZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RiskZoneRepository extends JpaRepository<RiskZone, Long> {

    /**
     * 주어진 좌표 범위(바운딩 박스) 안의 위험 구간 조회
     * 경로의 최소/최대 좌표에 마진을 더해서 호출
     */
    @Query("SELECT r FROM RiskZone r WHERE " +
            "r.latitude BETWEEN :minLat AND :maxLat AND " +
            "r.longitude BETWEEN :minLng AND :maxLng")
    List<RiskZone> findInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );
}
