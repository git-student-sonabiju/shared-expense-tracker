package com.sharedexpenses.repository;

import com.sharedexpenses.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("""
            select s from Settlement s
            join fetch s.fromMember
            join fetch s.toMember
            where s.group.id = :groupId
            order by s.createdAt desc, s.id desc
            """)
    List<Settlement> findAllByGroupId(@Param("groupId") Long groupId);

    Optional<Settlement> findByIdAndGroupId(Long id, Long groupId);

    @Modifying
    @Query("delete from Settlement s where s.group.id = :groupId")
    int deleteByGroupId(@Param("groupId") Long groupId);

    /** Rows of [memberId, totalSentCents]. */
    @Query("select s.fromMember.id, sum(s.amountCents) from Settlement s where s.group.id = :groupId group by s.fromMember.id")
    List<Object[]> sumSentByMember(@Param("groupId") Long groupId);

    /** Rows of [memberId, totalReceivedCents]. */
    @Query("select s.toMember.id, sum(s.amountCents) from Settlement s where s.group.id = :groupId group by s.toMember.id")
    List<Object[]> sumReceivedByMember(@Param("groupId") Long groupId);
}
