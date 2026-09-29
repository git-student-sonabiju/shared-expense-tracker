package com.sharedexpenses.service;

import com.sharedexpenses.domain.ExpenseGroup;
import com.sharedexpenses.domain.Member;
import com.sharedexpenses.domain.Settlement;
import com.sharedexpenses.repository.MemberRepository;
import com.sharedexpenses.repository.SettlementRepository;
import com.sharedexpenses.web.dto.Requests;
import com.sharedexpenses.web.dto.Responses;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SettlementService {

    private final GroupService groupService;
    private final SettlementRepository settlements;
    private final MemberRepository members;

    public SettlementService(GroupService groupService, SettlementRepository settlements, MemberRepository members) {
        this.groupService = groupService;
        this.settlements = settlements;
        this.members = members;
    }

    @Transactional(readOnly = true)
    public List<Responses.SettlementInfo> listSettlements(Long groupId) {
        groupService.requireGroup(groupId);
        return settlements.findAllByGroupId(groupId).stream().map(Responses.SettlementInfo::from).toList();
    }

    public Responses.SettlementInfo recordSettlement(Long groupId, Requests.CreateSettlement request) {
        ExpenseGroup group = groupService.requireGroup(groupId);
        if (request.fromMemberId().equals(request.toMemberId())) {
            throw new InvalidRequestException("toMemberId", "must be different from the payer");
        }
        Member from = members.findByIdAndGroupId(request.fromMemberId(), groupId)
                .orElseThrow(() -> new InvalidRequestException("fromMemberId", "must be a member of this group"));
        Member to = members.findByIdAndGroupId(request.toMemberId(), groupId)
                .orElseThrow(() -> new InvalidRequestException("toMemberId", "must be a member of this group"));
        long cents = Money.toCents(request.amount(), "amount");
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();

        return Responses.SettlementInfo.from(settlements.save(new Settlement(group, from, to, cents, note)));
    }

    public void deleteSettlement(Long groupId, Long settlementId) {
        groupService.requireGroup(groupId);
        Settlement settlement = settlements.findByIdAndGroupId(settlementId, groupId)
                .orElseThrow(() -> new NotFoundException("Settlement " + settlementId + " not found in group " + groupId));
        settlements.delete(settlement);
    }
}
