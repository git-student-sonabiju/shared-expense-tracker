package com.sharedexpenses.web;

import com.sharedexpenses.service.BalanceService;
import com.sharedexpenses.service.ExpenseService;
import com.sharedexpenses.service.GroupService;
import com.sharedexpenses.service.SettlementService;
import com.sharedexpenses.web.dto.Requests;
import com.sharedexpenses.web.dto.Responses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final ExpenseService expenseService;
    private final SettlementService settlementService;
    private final BalanceService balanceService;

    public GroupController(GroupService groupService, ExpenseService expenseService,
                           SettlementService settlementService, BalanceService balanceService) {
        this.groupService = groupService;
        this.expenseService = expenseService;
        this.settlementService = settlementService;
        this.balanceService = balanceService;
    }

    // ---- Groups & members ----

    @GetMapping
    public List<Responses.GroupSummary> listGroups() {
        return groupService.listGroups();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.Group createGroup(@Valid @RequestBody Requests.CreateGroup request) {
        return groupService.createGroup(request);
    }

    @GetMapping("/{groupId}")
    public Responses.Group getGroup(@PathVariable Long groupId) {
        return groupService.getGroup(groupId);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Long groupId) {
        groupService.deleteGroup(groupId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.MemberInfo addMember(@PathVariable Long groupId, @Valid @RequestBody Requests.AddMember request) {
        return groupService.addMember(groupId, request);
    }

    @PatchMapping("/{groupId}/members/{memberId}")
    public Responses.MemberInfo renameMember(@PathVariable Long groupId, @PathVariable Long memberId,
                                             @Valid @RequestBody Requests.RenameMember request) {
        return groupService.renameMember(groupId, memberId, request);
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        groupService.removeMember(groupId, memberId);
    }

    // ---- Expenses ----

    @GetMapping("/{groupId}/expenses")
    public List<Responses.ExpenseInfo> listExpenses(@PathVariable Long groupId) {
        return expenseService.listExpenses(groupId);
    }

    @PostMapping("/{groupId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.ExpenseInfo addExpense(@PathVariable Long groupId, @Valid @RequestBody Requests.CreateExpense request) {
        return expenseService.addExpense(groupId, request);
    }

    @DeleteMapping("/{groupId}/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long groupId, @PathVariable Long expenseId) {
        expenseService.deleteExpense(groupId, expenseId);
    }

    // ---- Settlements ----

    @GetMapping("/{groupId}/settlements")
    public List<Responses.SettlementInfo> listSettlements(@PathVariable Long groupId) {
        return settlementService.listSettlements(groupId);
    }

    @PostMapping("/{groupId}/settlements")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.SettlementInfo recordSettlement(@PathVariable Long groupId,
                                                     @Valid @RequestBody Requests.CreateSettlement request) {
        return settlementService.recordSettlement(groupId, request);
    }

    @DeleteMapping("/{groupId}/settlements/{settlementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSettlement(@PathVariable Long groupId, @PathVariable Long settlementId) {
        settlementService.deleteSettlement(groupId, settlementId);
    }

    // ---- Balances ----

    @GetMapping("/{groupId}/balances")
    public Responses.Balances getBalances(@PathVariable Long groupId) {
        return balanceService.getBalances(groupId);
    }
}
