package com.cts.backend.treaty.repositories;


import com.cts.backend.treaty.entity.Treaty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TreatyRepository extends JpaRepository<Treaty, Long> {
    Optional<Treaty> findByTreatyId(String treatyId);
    boolean existsByTreatyId(String treatyId);


    @Query("""
    select t
      from Treaty t
     where coalesce(t.renewalDate, t.endDate) >= :fromDate
       and coalesce(t.renewalDate, t.endDate) <= :toDate
       and t.status in :statuses
     order by coalesce(t.renewalDate, t.endDate) asc
""")
    List<Treaty> findUpcomingRenewals(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") List<Treaty.TreatyStatus> statuses
    );

}
