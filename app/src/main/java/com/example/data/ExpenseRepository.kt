package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class ExpenseRepository(private val dao: ExpenseDao) {

    val allGroups: Flow<List<GroupEntity>> = dao.getAllGroupsFlow()
    val allExpenses: Flow<List<ExpenseEntity>> = dao.getAllExpensesFlow()
    val allSplits: Flow<List<ExpenseSplitEntity>> = dao.getAllSplitsFlow()
    val allPayments: Flow<List<PaymentEntity>> = dao.getAllPaymentsFlow()

    fun getMembersForGroupFlow(groupId: Int): Flow<List<MemberEntity>> = dao.getMembersForGroupFlow(groupId)
    fun getExpensesForGroupFlow(groupId: Int): Flow<List<ExpenseEntity>> = dao.getExpensesForGroupFlow(groupId)
    fun getPaymentsForGroupFlow(groupId: Int): Flow<List<PaymentEntity>> = dao.getPaymentsForGroupFlow(groupId)

    suspend fun getGroupById(groupId: Int): GroupEntity? = dao.getGroupById(groupId)
    suspend fun getMembersForGroup(groupId: Int): List<MemberEntity> = dao.getMembersForGroup(groupId)
    suspend fun getExpensesForGroup(groupId: Int): List<ExpenseEntity> = dao.getExpensesForGroup(groupId)
    suspend fun getPaymentsForGroup(groupId: Int): List<PaymentEntity> = dao.getPaymentsForGroup(groupId)
    suspend fun getSplitsForExpense(expenseId: Int): List<ExpenseSplitEntity> = dao.getSplitsForExpense(expenseId)

    // --- WRITE OPERATIONS ---

    suspend fun createGroup(group: GroupEntity, memberNames: List<String>): Int {
        val groupId = dao.insertGroup(group).toInt()
        
        // Add "You" as creator
        val youMemberId = dao.insertMember(
            MemberEntity(
                groupId = groupId,
                name = "You (You (Demo))",
                isYou = true
            )
        ).toInt()

        // Add other members
        memberNames.forEach { name ->
            if (name.isNotBlank()) {
                dao.insertMember(
                    MemberEntity(
                        groupId = groupId,
                        name = name.trim(),
                        isYou = false
                    )
                )
            }
        }
        return groupId
    }

    suspend fun updateGroup(group: GroupEntity) {
        dao.updateGroup(group)
    }

    suspend fun deleteGroup(groupId: Int) {
        dao.deleteGroupById(groupId)
    }

    suspend fun addExpenseWithSplits(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>
    ): Int {
        val expenseId = dao.insertExpense(expense).toInt()
        val splitsToInsert = splits.map { it.copy(expenseId = expenseId) }
        dao.insertSplits(splitsToInsert)
        return expenseId
    }

    suspend fun updateExpenseWithSplits(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>
    ) {
        dao.updateExpense(expense)
        dao.deleteSplitsForExpense(expense.id)
        val splitsToInsert = splits.map { it.copy(expenseId = expense.id) }
        dao.insertSplits(splitsToInsert)
    }

    suspend fun deleteExpense(expenseId: Int) {
        dao.deleteSplitsForExpense(expenseId)
        dao.deleteExpenseById(expenseId)
    }

    suspend fun addPayment(payment: PaymentEntity): Int {
        return dao.insertPayment(payment).toInt()
    }

    suspend fun updatePayment(payment: PaymentEntity) {
        dao.updatePayment(payment)
    }

    suspend fun deletePayment(paymentId: Int) {
        dao.deletePaymentById(paymentId)
    }

    // --- SEEDING OPERATIONS ---

    suspend fun seedDemoData() {
        dao.clearAllGroups()

        // Time calculations
        val calendar = Calendar.getInstance()
        calendar.set(2026, Calendar.JULY, 6, 5, 30, 0)
        val dateJuly6_0530 = calendar.timeInMillis

        calendar.set(2026, Calendar.JULY, 6, 1, 53, 0)
        val dateJuly6_0153 = calendar.timeInMillis

        calendar.set(2026, Calendar.JULY, 7, 2, 24, 0)
        val dateJuly7_0224 = calendar.timeInMillis

        // Group 1: Trip2Goa
        val group1Id = dao.insertGroup(
            GroupEntity(
                name = "Trip2Goa",
                description = "Trip with roommates to Goa beach, summer 2026.",
                currencySymbol = "$"
            )
        ).toInt()

        val g1You = dao.insertMember(MemberEntity(groupId = group1Id, name = "You (You (Demo))", isYou = true)).toInt()
        val g1Avik = dao.insertMember(MemberEntity(groupId = group1Id, name = "Avik", isYou = false)).toInt()
        val g1Him = dao.insertMember(MemberEntity(groupId = group1Id, name = "Him", isYou = false)).toInt()

        // Expense 1: Flight
        val expense1Id = dao.insertExpense(
            ExpenseEntity(
                groupId = group1Id,
                description = "Flight",
                amount = 18000.00,
                date = dateJuly6_0530,
                paidByMemberId = g1You,
                splitMethod = "EQUAL"
            )
        ).toInt()

        dao.insertSplits(
            listOf(
                ExpenseSplitEntity(expenseId = expense1Id, memberId = g1You, amount = 6000.00, isSelected = true),
                ExpenseSplitEntity(expenseId = expense1Id, memberId = g1Avik, amount = 6000.00, isSelected = true),
                ExpenseSplitEntity(expenseId = expense1Id, memberId = g1Him, amount = 6000.00, isSelected = true)
            )
        )

        // Expense 2: Dinner
        val expense2Id = dao.insertExpense(
            ExpenseEntity(
                groupId = group1Id,
                description = "Dinner",
                amount = 1250.00,
                date = dateJuly6_0530,
                paidByMemberId = g1Him,
                splitMethod = "CUSTOM"
            )
        ).toInt()

        dao.insertSplits(
            listOf(
                ExpenseSplitEntity(expenseId = expense2Id, memberId = g1You, amount = 450.00, isSelected = true),
                ExpenseSplitEntity(expenseId = expense2Id, memberId = g1Avik, amount = 400.00, isSelected = true),
                ExpenseSplitEntity(expenseId = expense2Id, memberId = g1Him, amount = 400.00, isSelected = true)
            )
        )

        // Payment 1: Avik paid You
        dao.insertPayment(
            PaymentEntity(
                groupId = group1Id,
                fromMemberId = g1Avik,
                toMemberId = g1You,
                amount = 2400.00,
                date = dateJuly7_0224,
                status = "COMPLETED"
            )
        )

        // Group 2: Friday Board Games 🎲
        val group2Id = dao.insertGroup(
            GroupEntity(
                name = "Friday Board Games 🎲",
                description = "Weekly food, pizza and snack splits during our game sessions.",
                currencySymbol = "$"
            )
        ).toInt()

        val g2You = dao.insertMember(MemberEntity(groupId = group2Id, name = "You (You (Demo))", isYou = true)).toInt()
        val g2Bob = dao.insertMember(MemberEntity(groupId = group2Id, name = "Bob", isYou = false)).toInt()
        val g2Charlie = dao.insertMember(MemberEntity(groupId = group2Id, name = "Charlie", isYou = false)).toInt()

        val expense3Id = dao.insertExpense(
            ExpenseEntity(
                groupId = group2Id,
                description = "Snacks, Sodas, and Board game buy-in",
                amount = 24.00,
                date = dateJuly6_0153,
                paidByMemberId = g2You,
                splitMethod = "EQUAL"
            )
        ).toInt()

        dao.insertSplits(
            listOf(
                ExpenseSplitEntity(expenseId = expense3Id, memberId = g2You, amount = 8.00, isSelected = true),
                ExpenseSplitEntity(expenseId = expense3Id, memberId = g2Bob, amount = 8.00, isSelected = true),
                ExpenseSplitEntity(expenseId = expense3Id, memberId = g2Charlie, amount = 8.00, isSelected = true)
            )
        )

        // Group 3: Kyoto Autumn Trip 🍁
        val group3Id = dao.insertGroup(
            GroupEntity(
                name = "Kyoto Autumn Trip 🍁",
                description = "Our amazing vacation splitting hotels, delicious food, and bullet train tickets.",
                currencySymbol = "$"
            )
        ).toInt()

        dao.insertMember(MemberEntity(groupId = group3Id, name = "You (You (Demo))", isYou = true))
        dao.insertMember(MemberEntity(groupId = group3Id, name = "Ken", isYou = false))
        dao.insertMember(MemberEntity(groupId = group3Id, name = "Yuki", isYou = false))

        // Group 4: Roommates 2B 🏠
        val group4Id = dao.insertGroup(
            GroupEntity(
                name = "Roommates 2B 🏠",
                description = "Sharing rent, utilities, and grocery bills among room 2B mates.",
                currencySymbol = "$"
            )
        ).toInt()

        val g4You = dao.insertMember(MemberEntity(groupId = group4Id, name = "You (You (Demo))", isYou = true)).toInt()
        val g4John = dao.insertMember(MemberEntity(groupId = group4Id, name = "John (Room B)", isYou = false)).toInt()
        val g4Lily = dao.insertMember(MemberEntity(groupId = group4Id, name = "Lily (Room A)", isYou = false)).toInt()

        dao.insertPayment(
            PaymentEntity(
                groupId = group4Id,
                fromMemberId = g4John,
                toMemberId = g4Lily,
                amount = 640.00,
                date = dateJuly6_0153,
                status = "PENDING_APPROVAL"
            )
        )
    }
}
