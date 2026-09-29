package com.sharedexpenses.repository;

import com.sharedexpenses.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /** Loads expenses with payer and shares in one query to avoid N+1 selects. */
    @Query("""
            select distinct e from Expense e
            join fetch e.paidBy
            left join fetch e.shares s
            left join fetch s.member
            where e.group.id = :groupId
            order by e.createdAt desc, e.id desc
            """)
    List<Expense> findAllWithSharesByGroupId(@Param("groupId") Long groupId);

    Optional<Expense> findByIdAndGroupId(Long id, Long groupId);

    @Modifying
    @Query("delete from ExpenseShare s where s.expense.id in (select e.id from Expense e where e.group.id = :groupId)")
    int deleteSharesByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("delete from Expense e where e.group.id = :groupId")
    int deleteByGroupId(@Param("groupId") Long groupId);

    /** Rows of [memberId, totalPaidCents]. */
    @Query("select e.paidBy.id, sum(e.amountCents) from Expense e where e.group.id = :groupId group by e.paidBy.id")
    List<Object[]> sumPaidByMember(@Param("groupId") Long groupId);

    /** Rows of [memberId, totalOwedCents]. */
    @Query("""
            select s.member.id, sum(s.amountCents) from ExpenseShare s
            where s.expense.group.id = :groupId group by s.member.id
            """)
    List<Object[]> sumOwedByMember(@Param("groupId") Long groupId);
}
