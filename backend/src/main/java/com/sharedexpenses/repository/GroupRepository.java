package com.sharedexpenses.repository;

import com.sharedexpenses.domain.ExpenseGroup;
import com.sharedexpenses.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<ExpenseGroup, Long> {

    interface GroupSummaryView {
        Long getId();

        String getName();

        java.time.Instant getCreatedAt();

        long getMemberCount();
    }

    @Query("""
            select g.id as id, g.name as name, g.createdAt as createdAt, count(m.id) as memberCount
            from ExpenseGroup g left join g.members m
            where g.owner.id = :ownerId
            group by g.id, g.name, g.createdAt
            order by g.createdAt desc, g.id desc
            """)
    List<GroupSummaryView> findAllSummaries(@Param("ownerId") Long ownerId);

    Optional<ExpenseGroup> findByIdAndOwnerId(Long id, Long ownerId);

    @Modifying
    @Query("update ExpenseGroup g set g.owner = :owner where g.owner is null")
    int assignOrphanGroupsTo(@Param("owner") UserAccount owner);
}
