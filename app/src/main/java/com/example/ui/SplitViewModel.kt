package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SplitViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        ExpenseDatabase::class.java,
        "splitb_db"
    ).build()

    private val repository = ExpenseRepository(db.expenseDao())

    // --- AUTHENTICATION STATE ---
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserEmail = MutableStateFlow("guest@splitshare.local")
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    private val _currentUserName = MutableStateFlow("You (Demo)")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _preferredCurrency = MutableStateFlow("$")
    val preferredCurrency: StateFlow<String> = _preferredCurrency.asStateFlow()

    // --- CURRENT NAVIGATION / DETAIL STATE ---
    private val _activeGroupId = MutableStateFlow<Int?>(null)
    val activeGroupId: StateFlow<Int?> = _activeGroupId.asStateFlow()

    // Toast/Notification state for UI reminders
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        // Automatically seed demo data on first launch so the app feels alive
        viewModelScope.launch {
            val groups = repository.allGroups.first()
            if (groups.isEmpty()) {
                repository.seedDemoData()
            }
        }
    }

    fun login(email: String) {
        _currentUserEmail.value = email.ifBlank { "guest@splitshare.local" }
        _currentUserName.value = if (email.isNotBlank() && email.contains("@")) email.substringBefore("@") else "You (Demo)"
        _isLoggedIn.value = true
        showToast("Welcome back, ${_currentUserName.value}!")
    }

    fun registerAndLogin(email: String, name: String) {
        _currentUserEmail.value = email.ifBlank { "guest@splitshare.local" }
        _currentUserName.value = name.ifBlank { if (email.contains("@")) email.substringBefore("@") else "You" }
        _isLoggedIn.value = true
        showToast("Welcome to SplitB, ${_currentUserName.value}!")
    }

    fun loginAsDemo() {
        _currentUserEmail.value = "guest@splitshare.local"
        _currentUserName.value = "You (Demo)"
        _isLoggedIn.value = true
        showToast("Signed in as Demo Guest!")
    }

    fun logout() {
        _isLoggedIn.value = false
        showToast("Signed out successfully.")
    }

    fun updateProfile(name: String, email: String, currency: String) {
        _currentUserName.value = name.ifBlank { "You" }
        _currentUserEmail.value = email.ifBlank { "guest@splitshare.local" }
        _preferredCurrency.value = currency
        showToast("Profile & preferred currency updated!")
    }

    fun setActiveGroupId(groupId: Int?) {
        _activeGroupId.value = groupId
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(message)
        }
    }

    fun refreshDemoData() {
        viewModelScope.launch {
            repository.seedDemoData()
            showToast("Demo database reset and seeded!")
        }
    }

    // --- REACTIVE DATA COMBINATIONS ---

    // 1. Fetching active group details
    val activeGroup: StateFlow<GroupEntity?> = _activeGroupId.flatMapLatest { id ->
        if (id == null) flowOf<GroupEntity?>(null)
        else flow { emit(repository.getGroupById(id)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeGroupMembers: StateFlow<List<MemberEntity>> = _activeGroupId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getMembersForGroupFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGroupExpensesRaw: Flow<List<ExpenseEntity>> = _activeGroupId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getExpensesForGroupFlow(id)
    }

    val activeGroupPaymentsRaw: Flow<List<PaymentEntity>> = _activeGroupId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getPaymentsForGroupFlow(id)
    }

    // 2. Calculations for Active Group net balances
    val activeGroupNetBalances: StateFlow<List<MemberNetBalance>> = combine(
        activeGroupMembers,
        activeGroupExpensesRaw,
        activeGroupPaymentsRaw,
        repository.allSplits
    ) { members, expenses, payments, splits ->
        if (members.isEmpty()) return@combine emptyList()

        val memberMap = members.associateBy { it.id }
        
        // Initialize spent and share totals
        val spentMap = members.associate { it.id to 0.0 }.toMutableMap()
        val shareMap = members.associate { it.id to 0.0 }.toMutableMap()
        val netMap = members.associate { it.id to 0.0 }.toMutableMap()

        // 1. Process Expenses
        expenses.forEach { expense ->
            val paidBy = expense.paidByMemberId
            if (spentMap.containsKey(paidBy)) {
                spentMap[paidBy] = spentMap[paidBy]!! + expense.amount
            }

            // Get splits for this expense
            val expenseSplits = splits.filter { it.expenseId == expense.id && it.isSelected }
            expenseSplits.forEach { split ->
                if (shareMap.containsKey(split.memberId)) {
                    shareMap[split.memberId] = shareMap[split.memberId]!! + split.amount
                }
            }
        }

        // Calculate initial net balances from expenses
        members.forEach { m ->
            netMap[m.id] = spentMap[m.id]!! - shareMap[m.id]!!
        }

        // 2. Process Payments (only COMPLETED payments change balances)
        payments.filter { it.status == "COMPLETED" }.forEach { payment ->
            val fromId = payment.fromMemberId
            val toId = payment.toMemberId
            if (netMap.containsKey(fromId)) {
                netMap[fromId] = netMap[fromId]!! + payment.amount
            }
            if (netMap.containsKey(toId)) {
                netMap[toId] = netMap[toId]!! - payment.amount
            }
        }

        members.map { m ->
            MemberNetBalance(
                member = m,
                spent = spentMap[m.id] ?: 0.0,
                share = shareMap[m.id] ?: 0.0,
                net = netMap[m.id] ?: 0.0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Simplified Settlements
    val activeGroupSettlements: StateFlow<List<SimplifiedSettlement>> = activeGroupNetBalances.map { netBalances ->
        if (netBalances.isEmpty()) return@map emptyList()

        val debtors = netBalances.filter { it.net < -0.01 }.map { it.member to -it.net }.toMutableList()
        val creditors = netBalances.filter { it.net > 0.01 }.map { it.member to it.net }.toMutableList()

        val settlements = mutableListOf<SimplifiedSettlement>()

        var debtorIdx = 0
        var creditorIdx = 0

        val activeDebtors = debtors.map { Pair(it.first, it.second) }.toMutableList()
        val activeCreditors = creditors.map { Pair(it.first, it.second) }.toMutableList()

        while (debtorIdx < activeDebtors.size && creditorIdx < activeCreditors.size) {
            val debtor = activeDebtors[debtorIdx]
            val creditor = activeCreditors[creditorIdx]

            val amountToSettle = minOf(debtor.second, creditor.second)
            if (amountToSettle > 0.01) {
                settlements.add(
                    SimplifiedSettlement(
                        fromMember = debtor.first,
                        toMember = creditor.first,
                        amount = amountToSettle
                    )
                )
            }

            activeDebtors[debtorIdx] = Pair(debtor.first, debtor.second - amountToSettle)
            activeCreditors[creditorIdx] = Pair(creditor.first, creditor.second - amountToSettle)

            if (activeDebtors[debtorIdx].second < 0.01) {
                debtorIdx++
            }
            if (activeCreditors[creditorIdx].second < 0.01) {
                creditorIdx++
            }
        }

        settlements
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Expenses with detail strings and split shares for display
    val activeGroupExpenses: StateFlow<List<ExpenseWithDetails>> = combine(
        activeGroupExpensesRaw,
        activeGroupMembers,
        repository.allSplits
    ) { expenses, members, splits ->
        if (members.isEmpty()) return@combine emptyList()
        val memberMap = members.associateBy { it.id }
        val youMember = members.find { it.isYou }

        expenses.map { expense ->
            val paidBy = memberMap[expense.paidByMemberId] ?: MemberEntity(name = "Unknown", groupId = expense.groupId, isYou = false)
            val expenseSplits = splits.filter { it.expenseId == expense.id }
            
            val splitsWithMember = expenseSplits.map { split ->
                ExpenseSplitWithMember(
                    split = split,
                    member = memberMap[split.memberId] ?: MemberEntity(name = "Unknown", groupId = expense.groupId, isYou = false)
                )
            }

            val totalPeople = expenseSplits.count { it.isSelected }
            val splitText = if (expense.splitMethod == "EQUAL") {
                "Split equally with $totalPeople people"
            } else {
                "Custom split with $totalPeople people"
            }

            val detailText = "Paid by ${if (paidBy.isYou) "You" else paidBy.name} • $splitText"

            // Calculate Lent / Owe for You
            var lentAmount = 0.0
            var oweAmount = 0.0

            if (youMember != null) {
                val youSplit = expenseSplits.find { it.memberId == youMember.id && it.isSelected }
                val youPaid = expense.paidByMemberId == youMember.id

                if (youPaid) {
                    // You paid. What others owe You is the sum of their shares
                    val othersShare = expenseSplits.filter { it.memberId != youMember.id && it.isSelected }.sumOf { it.amount }
                    lentAmount = othersShare
                } else if (youSplit != null) {
                    // Someone else paid, and You are included in split. You owe them your split share
                    oweAmount = youSplit.amount
                }
            }

            ExpenseWithDetails(
                expense = expense,
                paidBy = paidBy,
                splits = splitsWithMember,
                lentAmount = lentAmount,
                oweAmount = oweAmount,
                detailText = detailText
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Payments with details
    val activeGroupPayments: StateFlow<List<PaymentWithDetails>> = combine(
        activeGroupPaymentsRaw,
        activeGroupMembers
    ) { payments, members ->
        if (members.isEmpty()) return@combine emptyList()
        val memberMap = members.associateBy { it.id }

        payments.map { p ->
            PaymentWithDetails(
                payment = p,
                fromMember = memberMap[p.fromMemberId] ?: MemberEntity(name = "Unknown", groupId = p.groupId, isYou = false),
                toMember = memberMap[p.toMemberId] ?: MemberEntity(name = "Unknown", groupId = p.groupId, isYou = false)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Groups with combined stats for Dashboard
    val groupsWithStats: StateFlow<List<GroupWithStats>> = combine(
        repository.allGroups,
        repository.allExpenses,
        repository.allPayments,
        repository.allSplits
    ) { groups, expenses, payments, splits ->
        
        groups.map { group ->
            val members = repository.getMembersForGroup(group.id)
            val groupExpenses = expenses.filter { it.groupId == group.id }
            val groupPayments = payments.filter { it.groupId == group.id }

            val memberMap = members.associateBy { it.id }
            val youMember = members.find { it.isYou }

            var totalSpent = groupExpenses.sumOf { it.amount }
            var youOwe = 0.0
            var youAreOwed = 0.0
            var netBalanceForYou = 0.0

            if (youMember != null) {
                // Initialize net map
                val netMap = members.associate { it.id to 0.0 }.toMutableMap()

                groupExpenses.forEach { exp ->
                    val paidBy = exp.paidByMemberId
                    val expSplits = splits.filter { it.expenseId == exp.id && it.isSelected }
                    expSplits.forEach { split ->
                        netMap[paidBy] = (netMap[paidBy] ?: 0.0) + split.amount
                        netMap[split.memberId] = (netMap[split.memberId] ?: 0.0) - split.amount
                    }
                }

                groupPayments.filter { it.status == "COMPLETED" }.forEach { pay ->
                    netMap[pay.fromMemberId] = (netMap[pay.fromMemberId] ?: 0.0) + pay.amount
                    netMap[pay.toMemberId] = (netMap[pay.toMemberId] ?: 0.0) - pay.amount
                }

                netBalanceForYou = netMap[youMember.id] ?: 0.0
                if (netBalanceForYou > 0.01) {
                    youAreOwed = netBalanceForYou
                } else if (netBalanceForYou < -0.01) {
                    youOwe = -netBalanceForYou
                }
            }

            val statusText = when {
                netBalanceForYou > 0.01 -> "You are owed ${group.currencySymbol}${String.format("%.2f", netBalanceForYou)}"
                netBalanceForYou < -0.01 -> "You owe ${group.currencySymbol}${String.format("%.2f", -netBalanceForYou)}"
                else -> "Settled up"
            }

            GroupWithStats(
                group = group,
                members = members,
                billCount = groupExpenses.size,
                totalSpent = totalSpent,
                youOwe = youOwe,
                youAreOwed = youAreOwed,
                netBalanceForYou = netBalanceForYou,
                statusText = statusText
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7. Overall Dashboard Statistics
    val dashboardOverviewStats = groupsWithStats.map { list ->
        val totalOwed = list.sumOf { it.youAreOwed }
        val totalOwe = list.sumOf { it.youOwe }
        val netBalance = totalOwed - totalOwe
        Triple(netBalance, totalOwed, totalOwe)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0.0, 0.0, 0.0))

    // 8. Recent activities across ALL groups
    val recentActivities: StateFlow<List<ActivityItem>> = combine(
        repository.allGroups,
        repository.allExpenses,
        repository.allPayments
    ) { groups, expenses, payments ->
        val groupMap = groups.associateBy { it.id }
        
        val items = mutableListOf<ActivityItem>()

        // Process expenses
        expenses.forEach { exp ->
            val group = groupMap[exp.groupId] ?: return@forEach
            val members = repository.getMembersForGroup(group.id)
            val memberMap = members.associateBy { it.id }
            val paidBy = memberMap[exp.paidByMemberId]

            val paidByName = when {
                paidBy == null -> "Unknown"
                paidBy.isYou -> "You (${_currentUserName.value})"
                else -> paidBy.name
            }

            items.add(
                ActivityItem(
                    id = "expense_${exp.id}",
                    title = "$paidByName added \"${exp.description}\"",
                    subtitle = group.name,
                    amount = exp.amount,
                    dateMillis = exp.date,
                    statusText = "",
                    isPayment = false,
                    groupName = group.name,
                    groupId = group.id,
                    fromName = paidByName,
                    toName = "",
                    icon = "💵",
                    currencySymbol = group.currencySymbol
                )
            )
        }

        // Process payments
        payments.forEach { p ->
            val group = groupMap[p.groupId] ?: return@forEach
            val members = repository.getMembersForGroup(group.id)
            val memberMap = members.associateBy { it.id }
            val fromMember = memberMap[p.fromMemberId]
            val toMember = memberMap[p.toMemberId]

            val fromName = when {
                fromMember == null -> "Unknown"
                fromMember.isYou -> "You (${_currentUserName.value})"
                else -> fromMember.name
            }

            val toName = when {
                toMember == null -> "Unknown"
                toMember.isYou -> "You (${_currentUserName.value})"
                else -> toMember.name
            }

            val statusText = if (p.status == "PENDING_APPROVAL") "(Pending)" else ""

            items.add(
                ActivityItem(
                    id = "payment_${p.id}",
                    title = "$fromName sent payment to $toName",
                    subtitle = group.name,
                    amount = p.amount,
                    dateMillis = p.date,
                    statusText = statusText,
                    isPayment = true,
                    groupName = group.name,
                    groupId = group.id,
                    fromName = fromName,
                    toName = toName,
                    icon = "↗",
                    currencySymbol = group.currencySymbol
                )
            )
        }

        items.sortedByDescending { it.dateMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- CRUD FUNCTIONS FOR SCREEN TRIGGERS ---

    fun createGroup(name: String, description: String, currency: String, members: List<String>) {
        viewModelScope.launch {
            val group = GroupEntity(name = name, description = description, currencySymbol = currency)
            val newGroupId = repository.createGroup(group, members)
            _activeGroupId.value = newGroupId
            showToast("Group \"$name\" created!")
        }
    }

    fun renameGroup(group: GroupEntity, newName: String) {
        viewModelScope.launch {
            val updated = group.copy(name = newName)
            repository.updateGroup(updated)
            showToast("Group renamed to \"$newName\"")
        }
    }

    fun deleteGroup(groupId: Int) {
        viewModelScope.launch {
            repository.deleteGroup(groupId)
            if (_activeGroupId.value == groupId) {
                _activeGroupId.value = null
            }
            showToast("Group deleted.")
        }
    }

    fun addExpense(
        description: String,
        amount: Double,
        date: Long,
        paidByMemberId: Int,
        splitMethod: String,
        selectedMemberIds: List<Int>,
        customAmounts: Map<Int, Double>
    ) {
        viewModelScope.launch {
            val groupId = _activeGroupId.value ?: return@launch
            val expense = ExpenseEntity(
                groupId = groupId,
                description = description,
                amount = amount,
                date = date,
                paidByMemberId = paidByMemberId,
                splitMethod = splitMethod
            )

            val splits = mutableListOf<ExpenseSplitEntity>()
            if (splitMethod == "EQUAL") {
                val perPerson = if (selectedMemberIds.isNotEmpty()) amount / selectedMemberIds.size else 0.0
                val allGroupMembers = repository.getMembersForGroup(groupId)
                allGroupMembers.forEach { member ->
                    val isSel = selectedMemberIds.contains(member.id)
                    splits.add(
                        ExpenseSplitEntity(
                            expenseId = 0,
                            memberId = member.id,
                            amount = if (isSel) perPerson else 0.0,
                            isSelected = isSel
                        )
                    )
                }
            } else {
                val allGroupMembers = repository.getMembersForGroup(groupId)
                allGroupMembers.forEach { member ->
                    val isSel = selectedMemberIds.contains(member.id)
                    val shareAmt = customAmounts[member.id] ?: 0.0
                    splits.add(
                        ExpenseSplitEntity(
                            expenseId = 0,
                            memberId = member.id,
                            amount = if (isSel) shareAmt else 0.0,
                            isSelected = isSel
                        )
                    )
                }
            }

            repository.addExpenseWithSplits(expense, splits)
            showToast("Expense \"$description\" added!")
        }
    }

    fun updateExpense(
        expenseId: Int,
        description: String,
        amount: Double,
        date: Long,
        paidByMemberId: Int,
        splitMethod: String,
        selectedMemberIds: List<Int>,
        customAmounts: Map<Int, Double>
    ) {
        viewModelScope.launch {
            val groupId = _activeGroupId.value ?: return@launch
            val expense = ExpenseEntity(
                id = expenseId,
                groupId = groupId,
                description = description,
                amount = amount,
                date = date,
                paidByMemberId = paidByMemberId,
                splitMethod = splitMethod
            )

            val splits = mutableListOf<ExpenseSplitEntity>()
            if (splitMethod == "EQUAL") {
                val perPerson = if (selectedMemberIds.isNotEmpty()) amount / selectedMemberIds.size else 0.0
                val allGroupMembers = repository.getMembersForGroup(groupId)
                allGroupMembers.forEach { member ->
                    val isSel = selectedMemberIds.contains(member.id)
                    splits.add(
                        ExpenseSplitEntity(
                            expenseId = expenseId,
                            memberId = member.id,
                            amount = if (isSel) perPerson else 0.0,
                            isSelected = isSel
                        )
                    )
                }
            } else {
                val allGroupMembers = repository.getMembersForGroup(groupId)
                allGroupMembers.forEach { member ->
                    val isSel = selectedMemberIds.contains(member.id)
                    val shareAmt = customAmounts[member.id] ?: 0.0
                    splits.add(
                        ExpenseSplitEntity(
                            expenseId = expenseId,
                            memberId = member.id,
                            amount = if (isSel) shareAmt else 0.0,
                            isSelected = isSel
                        )
                    )
                }
            }

            repository.updateExpenseWithSplits(expense, splits)
            showToast("Expense updated!")
        }
    }

    fun deleteExpense(expenseId: Int) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            showToast("Expense deleted.")
        }
    }

    fun addPayment(fromId: Int, toId: Int, amount: Double, status: String) {
        viewModelScope.launch {
            val groupId = _activeGroupId.value ?: return@launch
            val payment = PaymentEntity(
                groupId = groupId,
                fromMemberId = fromId,
                toMemberId = toId,
                amount = amount,
                date = System.currentTimeMillis(),
                status = status
            )
            repository.addPayment(payment)
            showToast("Payment recorded!")
        }
    }

    fun approvePayment(paymentId: Int) {
        viewModelScope.launch {
            val allPayments = repository.allPayments.first()
            val payment = allPayments.find { it.id == paymentId }
            if (payment != null) {
                repository.updatePayment(payment.copy(status = "COMPLETED"))
                showToast("Payment approved and completed!")
            }
        }
    }

    fun remindPayment(paymentId: Int) {
        viewModelScope.launch {
            showToast("Reminder notification sent to group member!")
        }
    }

    fun deletePayment(paymentId: Int) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
            showToast("Payment deleted.")
        }
    }
}
