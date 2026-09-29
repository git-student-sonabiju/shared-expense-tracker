package com.sharedexpenses.service;

import com.sharedexpenses.auth.CurrentUser;
import com.sharedexpenses.domain.ExpenseGroup;
import com.sharedexpenses.domain.Member;
import com.sharedexpenses.repository.ExpenseRepository;
import com.sharedexpenses.repository.GroupRepository;
import com.sharedexpenses.repository.MemberRepository;
import com.sharedexpenses.repository.SettlementRepository;
import com.sharedexpenses.repository.UserRepository;
import com.sharedexpenses.web.dto.Requests;
import com.sharedexpenses.web.dto.Responses;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class GroupService {

    private final GroupRepository groups;
    private final MemberRepository members;
    private final UserRepository users;
    private final ExpenseRepository expenses;
    private final SettlementRepository settlements;

    public GroupService(GroupRepository groups, MemberRepository members, UserRepository users,
                        ExpenseRepository expenses, SettlementRepository settlements) {
        this.groups = groups;
        this.members = members;
        this.users = users;
        this.expenses = expenses;
        this.settlements = settlements;
    }

    @Transactional(readOnly = true)
    public List<Responses.GroupSummary> listGroups() {
        return groups.findAllSummaries(CurrentUser.id()).stream()
                .map(g -> new Responses.GroupSummary(g.getId(), g.getName(), g.getMemberCount(), g.getCreatedAt()))
                .toList();
    }

    public Responses.Group createGroup(Requests.CreateGroup request) {
        ExpenseGroup group = new ExpenseGroup(request.name().trim(), users.getReferenceById(CurrentUser.id()));
        Set<String> seen = new HashSet<>();
        for (String raw : request.members() == null ? List.<String>of() : request.members()) {
            String name = raw.trim();
            if (!seen.add(name.toLowerCase(Locale.ROOT))) {
                throw new InvalidRequestException("members", "contains the name '" + name + "' more than once");
            }
            group.addMember(name);
        }
        return Responses.Group.from(groups.save(group));
    }

    @Transactional(readOnly = true)
    public Responses.Group getGroup(Long groupId) {
        return Responses.Group.from(requireGroup(groupId));
    }

    /**
     * Deletes the group and everything in it. Shares, expenses and settlements reference members,
     * so they are removed first with bulk deletes; the members then go with the group via cascade.
     */
    public void deleteGroup(Long groupId) {
        ExpenseGroup group = requireGroup(groupId);
        expenses.deleteSharesByGroupId(groupId);
        expenses.deleteByGroupId(groupId);
        settlements.deleteByGroupId(groupId);
        groups.delete(group);
    }

    public Responses.MemberInfo addMember(Long groupId, Requests.AddMember request) {
        ExpenseGroup group = requireGroup(groupId);
        String name = request.name().trim();
        if (members.existsByGroupIdAndNameIgnoreCase(groupId, name)) {
            throw new ConflictException("A member named '" + name + "' already exists in this group");
        }
        Member member = group.addMember(name);
        members.saveAndFlush(member);
        return Responses.MemberInfo.from(member);
    }

    /** Renaming keeps all history intact: expenses and settlements reference the member by id. */
    public Responses.MemberInfo renameMember(Long groupId, Long memberId, Requests.RenameMember request) {
        requireGroup(groupId);
        Member member = requireMember(groupId, memberId);
        String name = request.name().trim();
        boolean onlyCaseChanged = member.getName().equalsIgnoreCase(name);
        if (!onlyCaseChanged && members.existsByGroupIdAndNameIgnoreCase(groupId, name)) {
            throw new ConflictException("A member named '" + name + "' already exists in this group");
        }
        member.rename(name);
        members.flush();
        return Responses.MemberInfo.from(member);
    }

    /** Only members with no expenses or settlements can be removed, so history and balances stay intact. */
    public void removeMember(Long groupId, Long memberId) {
        ExpenseGroup group = requireGroup(groupId);
        Member member = requireMember(groupId, memberId);
        if (members.hasActivity(memberId)) {
            throw new ConflictException("'" + member.getName()
                    + "' is part of existing expenses or settlements and cannot be removed");
        }
        group.removeMember(member);
    }

    /** Groups owned by someone else are reported as not found, so their existence is not revealed. */
    ExpenseGroup requireGroup(Long groupId) {
        return groups.findByIdAndOwnerId(groupId, CurrentUser.id())
                .orElseThrow(() -> new NotFoundException("Group " + groupId + " not found"));
    }

    Member requireMember(Long groupId, Long memberId) {
        return members.findByIdAndGroupId(memberId, groupId)
                .orElseThrow(() -> new NotFoundException("Member " + memberId + " not found in group " + groupId));
    }
}
