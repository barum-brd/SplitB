package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- ENTITIES ---

@Entity(tableName = "expense_groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val currencySymbol: String
)

@Entity(
    tableName = "group_members",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val name: String,
    val isYou: Boolean
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val description: String,
    val amount: Double,
    val date: Long,
    val paidByMemberId: Int, // Refers to MemberEntity.id
    val splitMethod: String // "EQUAL" or "CUSTOM"
)

@Entity(
    tableName = "expense_splits",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("expenseId")]
)
data class ExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    val memberId: Int, // Refers to MemberEntity.id
    val amount: Double,
    val isSelected: Boolean
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val fromMemberId: Int, // Refers to MemberEntity.id
    val toMemberId: Int, // Refers to MemberEntity.id
    val amount: Double,
    val date: Long,
    val status: String // "COMPLETED" or "PENDING_APPROVAL"
)

// --- DAO ---

@Dao
interface ExpenseDao {

    // --- GROUPS ---
    @Query("SELECT * FROM expense_groups ORDER BY id DESC")
    fun getAllGroupsFlow(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM expense_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Int): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("DELETE FROM expense_groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: Int)

    // --- MEMBERS ---
    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun getMembersForGroupFlow(groupId: Int): Flow<List<MemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getMembersForGroup(groupId: Int): List<MemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Query("DELETE FROM group_members WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: Int)

    @Update
    suspend fun updateMember(member: MemberEntity)

    // --- EXPENSES ---
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    fun getExpensesForGroupFlow(groupId: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    suspend fun getExpensesForGroup(groupId: Int): List<ExpenseEntity>

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpensesFlow(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Int)

    // --- SPLITS ---
    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getSplitsForExpenseFlow(expenseId: Int): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpense(expenseId: Int): List<ExpenseSplitEntity>

    @Query("SELECT * FROM expense_splits")
    fun getAllSplitsFlow(): Flow<List<ExpenseSplitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: Int)

    // --- PAYMENTS ---
    @Query("SELECT * FROM payments WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    fun getPaymentsForGroupFlow(groupId: Int): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    suspend fun getPaymentsForGroup(groupId: Int): List<PaymentEntity>

    @Query("SELECT * FROM payments ORDER BY date DESC, id DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Int)

    // --- DATABASE CLEANUP FOR DEMO ---
    @Query("DELETE FROM expense_groups")
    suspend fun clearAllGroups()
}

// --- DATABASE ---

@Database(
    entities = [
        GroupEntity::class,
        MemberEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        PaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
}

// --- UI TRANSFER MODELS ---

data class MemberNetBalance(
    val member: MemberEntity,
    val spent: Double,
    val share: Double,
    val net: Double
)

data class SimplifiedSettlement(
    val fromMember: MemberEntity,
    val toMember: MemberEntity,
    val amount: Double
)

data class ExpenseSplitWithMember(
    val split: ExpenseSplitEntity,
    val member: MemberEntity
)

data class ExpenseWithDetails(
    val expense: ExpenseEntity,
    val paidBy: MemberEntity,
    val splits: List<ExpenseSplitWithMember>,
    val lentAmount: Double,
    val oweAmount: Double,
    val detailText: String
)

data class PaymentWithDetails(
    val payment: PaymentEntity,
    val fromMember: MemberEntity,
    val toMember: MemberEntity
)

data class GroupWithStats(
    val group: GroupEntity,
    val members: List<MemberEntity>,
    val billCount: Int,
    val totalSpent: Double,
    val youOwe: Double,
    val youAreOwed: Double,
    val netBalanceForYou: Double,
    val statusText: String
)

data class ActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val dateMillis: Long,
    val statusText: String,
    val isPayment: Boolean,
    val groupName: String,
    val groupId: Int,
    val fromName: String,
    val toName: String,
    val icon: String,
    val currencySymbol: String = "$"
)
