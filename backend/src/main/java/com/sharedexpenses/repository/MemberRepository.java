package com.sharedexpenses.repository;

import com.sharedexpenses.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByGroupIdOrderByIdAsc(Long groupId);

    Optional<Member> findByIdAndGroupId(Long id, Long groupId);

    boolean existsByGroupIdAndNameIgnoreCase(Long groupId, String name);

    /** True if the member appears in any expense (as payer or participant) or any settlement. */
    @Query("""
            select case when
                exists (select 1 from Expense e where e.paidBy.id = :memberId)
                or exists (select 1 from ExpenseShare s where s.member.id = :memberId)
                or exists (select 1 from Settlement st where st.fromMember.id = :memberId or st.toMember.id = :memberId)
            then true else false end
            """)
    boolean hasActivity(@Param("memberId") Long memberId);
}
