@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.*
import com.rumpadhar.splitb.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- MAIN COMPOSE APP WRAPPER ---

@Composable
fun SplitShareApp(viewModel: SplitViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showSplash by remember { mutableStateOf(true) }

    // Listen to toast messages from ViewModel
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Splash screen timer
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2200)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else if (!isLoggedIn) {
        AuthScreen(
            onLogin = { email -> viewModel.login(email) },
            onRegister = { email, name -> viewModel.registerAndLogin(email, name) },
            onGuestLogin = { viewModel.loginAsDemo() }
        )
    } else {
        MainAppLayout(
            viewModel = viewModel,
            snackbarHostState = snackbarHostState
        )
    }
}

@Composable
fun SplashScreen() {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        )
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        )
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131517)), // Matches dark slate background of generated logo
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .alpha(alphaAnim)
                .graphicsLayer(
                    scaleX = scaleAnim,
                    scaleY = scaleAnim
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_app_logo_1783531906531),
                contentDescription = "SplitB Logo",
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(2.dp, Color(0xFF049469), RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "SplitB",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Seamless bill sharing, simplified.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFA5B4FC),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CircularProgressIndicator(
                color = Color(0xFF049469),
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// --- AUTHENTICATION SCREEN ---

enum class AuthMode {
    LOGIN,
    REGISTER,
    VERIFICATION
}

@Composable
fun AuthScreen(
    onLogin: (String) -> Unit,
    onRegister: (String, String) -> Unit,
    onGuestLogin: () -> Unit
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("barum.brd@gmail.com") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("••••••••") }
    
    // Verification state
    var verificationCodeInput by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf("") }
    var verificationError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFC))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 450.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.img_app_logo_1783531906531),
                contentDescription = "SplitB Logo",
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, Color(0xFF049469), RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )

            Text(
                text = "SplitB",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1A1C19)
            )

            Text(
                text = "Split bills with friends, roommates, and travel groups.",
                fontSize = 14.sp,
                color = Color(0xFF424940),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (authMode) {
                        AuthMode.LOGIN -> {
                            Text(
                                text = "Sign In",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C19)
                            )

                            Text(
                                text = "Email Address",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C19)
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_input"),
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF386A20)) },
                                placeholder = { Text("e.g. email@example.com") },
                                textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF386A20),
                                    unfocusedBorderColor = Color(0xFFDDE5D9)
                                ),
                                singleLine = true
                            )

                            Text(
                                text = "Password",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C19)
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input"),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF386A20)) },
                                visualTransformation = PasswordVisualTransformation(),
                                textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF386A20),
                                    unfocusedBorderColor = Color(0xFFDDE5D9)
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { onLogin(email) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("signin_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F39F6)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(1.0f), color = Color(0xFFDDE5D9))
                                Text(
                                    text = "OR USE DEMO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF72796F),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                HorizontalDivider(modifier = Modifier.weight(1.0f), color = Color(0xFFDDE5D9))
                            }

                            OutlinedButton(
                                onClick = onGuestLogin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("demo_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A1C19)),
                                border = BorderStroke(1.dp, Color(0xFFDDE5D9)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "✨ Quick Guest / Seeded Demo Mode",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        AuthMode.REGISTER -> {
                            Text(
                                text = "Create Account",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C19)
                            )

                            Text(
                                text = "Full Name",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C19)
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_name_input"),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF386A20)) },
                                placeholder = { Text("e.g. Barum Kumar") },
                                textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF386A20),
                                    unfocusedBorderColor = Color(0xFFDDE5D9)
                                ),
                                singleLine = true
                            )

                            Text(
                                text = "Email Address",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C19)
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_email_input"),
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF386A20)) },
                                placeholder = { Text("e.g. barum@example.com") },
                                textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF386A20),
                                    unfocusedBorderColor = Color(0xFFDDE5D9)
                                ),
                                singleLine = true
                            )

                            Text(
                                text = "Password",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C19)
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_password_input"),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF386A20)) },
                                visualTransformation = PasswordVisualTransformation(),
                                textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF386A20),
                                    unfocusedBorderColor = Color(0xFFDDE5D9)
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (email.isNotBlank() && name.isNotBlank()) {
                                        // Generate 6-digit random code
                                        generatedCode = (100000..999999).random().toString()
                                        verificationCodeInput = ""
                                        verificationError = false
                                        authMode = AuthMode.VERIFICATION
                                    }
                                },
                                enabled = email.isNotBlank() && name.isNotBlank() && password.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("register_send_code_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F39F6)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Verification Code", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        AuthMode.VERIFICATION -> {
                            Text(
                                text = "Verify Your Email",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C19)
                            )

                            Text(
                                text = "A verification code has been sent to $email. Please enter it below to complete registration.",
                                fontSize = 13.sp,
                                color = Color(0xFF424940)
                            )

                            // Alert box showing generated code in Sandbox/Demo mode
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .border(1.dp, Color(0xFF81C784), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "DEMO SIMULATION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Verification code: $generatedCode",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }

                            Text(
                                text = "6-Digit Verification Code",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C19)
                            )

                            OutlinedTextField(
                                value = verificationCodeInput,
                                onValueChange = { 
                                    verificationCodeInput = it.take(6) 
                                    verificationError = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("verification_code_input"),
                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF386A20)) },
                                placeholder = { Text("e.g. 123456") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469), fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF386A20),
                                    unfocusedBorderColor = Color(0xFFDDE5D9)
                                ),
                                singleLine = true
                            )

                            if (verificationError) {
                                Text(
                                    text = "Invalid verification code. Please check the code and try again.",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (verificationCodeInput == generatedCode) {
                                        onRegister(email, name)
                                    } else {
                                        verificationError = true
                                    }
                                },
                                enabled = verificationCodeInput.length == 6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("verify_code_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386A20)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify & Register", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Resend Code",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF386A20),
                                    modifier = Modifier.clickable {
                                        generatedCode = (100000..999999).random().toString()
                                        verificationCodeInput = ""
                                        verificationError = false
                                    }
                                )

                                Text(
                                    text = "Change Email",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.clickable {
                                        authMode = AuthMode.REGISTER
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (authMode) {
                AuthMode.LOGIN -> {
                    Text(
                        text = "New to SplitB? Create Account",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF386A20),
                        modifier = Modifier
                            .clickable { authMode = AuthMode.REGISTER }
                            .testTag("toggle_to_register")
                    )
                }
                AuthMode.REGISTER -> {
                    Text(
                        text = "Already have an account? Sign In",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF386A20),
                        modifier = Modifier
                            .clickable { authMode = AuthMode.LOGIN }
                            .testTag("toggle_to_login")
                    )
                }
                AuthMode.VERIFICATION -> {
                    // Handled inside Card
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- MAIN LAYOUT WITH SIDEBAR DRAWER ---

@Composable
fun MainAppLayout(
    viewModel: SplitViewModel,
    snackbarHostState: SnackbarHostState
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val groupsWithStats by viewModel.groupsWithStats.collectAsStateWithLifecycle()
    val activeGroupId by viewModel.activeGroupId.collectAsStateWithLifecycle()
    val currentEmail by viewModel.currentUserEmail.collectAsStateWithLifecycle()
    val currentName by viewModel.currentUserName.collectAsStateWithLifecycle()
    val preferredCurrency by viewModel.preferredCurrency.collectAsStateWithLifecycle()

    // Dialog state controllers
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var settleUpPreFill by remember { mutableStateOf<SimplifiedSettlement?>(null) }
    var expenseToEdit by remember { mutableStateOf<ExpenseWithDetails?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1A1C19),
                modifier = Modifier.width(300.dp)
            ) {
                SidebarDrawerContent(
                    groups = groupsWithStats,
                    selectedGroupId = activeGroupId,
                    currentUserEmail = currentEmail,
                    currentUserName = currentName,
                    preferredCurrency = preferredCurrency,
                    onDashboardSelected = {
                        viewModel.setActiveGroupId(null)
                        scope.launch { drawerState.close() }
                    },
                    onGroupSelected = { id ->
                        viewModel.setActiveGroupId(id)
                        scope.launch { drawerState.close() }
                    },
                    onCreateGroupClick = {
                        showCreateGroupDialog = true
                        scope.launch { drawerState.close() }
                    },
                    onRefreshDemoClick = {
                        viewModel.refreshDemoData()
                        scope.launch { drawerState.close() }
                    },
                    onSignOutClick = {
                        viewModel.logout()
                    },
                    onUpdateProfile = { name, email, currency ->
                        viewModel.updateProfile(name, email, currency)
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_logo_1783531906531),
                                contentDescription = "SplitB Logo",
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF049469), RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SplitB",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF1A1C19)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF9FAFC))
            ) {
                if (activeGroupId == null) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onCreateGroupClick = { showCreateGroupDialog = true },
                        onGroupClick = { id -> viewModel.setActiveGroupId(id) }
                    )
                } else {
                    GroupDetailScreen(
                        viewModel = viewModel,
                        onBackToDashboard = { viewModel.setActiveGroupId(null) },
                        onAddExpenseClick = { showAddExpenseDialog = true },
                        onRecordPaymentClick = { showRecordPaymentDialog = true },
                        onSettleUpClick = { settlement ->
                            settleUpPreFill = settlement
                            showRecordPaymentDialog = true
                        },
                        onEditExpenseClick = { expense ->
                            expenseToEdit = expense
                            showAddExpenseDialog = true
                        }
                    )
                }
            }
        }
    }

    // --- POPUP DIALOGS ---

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            defaultCurrency = preferredCurrency,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, desc, currency, members ->
                viewModel.createGroup(name, desc, currency, members)
                showCreateGroupDialog = false
            }
        )
    }

    if (showAddExpenseDialog) {
        val members by viewModel.activeGroupMembers.collectAsStateWithLifecycle()
        val activeGroup by viewModel.activeGroup.collectAsStateWithLifecycle()
        val groupCurrency = activeGroup?.currencySymbol ?: "$"
        AddEditExpenseDialog(
            expenseToEdit = expenseToEdit,
            members = members,
            currency = groupCurrency,
            onDismiss = {
                showAddExpenseDialog = false
                expenseToEdit = null
            },
            onSave = { desc, amount, date, paidBy, splitMethod, selectedIds, customAmts ->
                if (expenseToEdit == null) {
                    viewModel.addExpense(desc, amount, date, paidBy, splitMethod, selectedIds, customAmts)
                } else {
                    viewModel.updateExpense(expenseToEdit!!.expense.id, desc, amount, date, paidBy, splitMethod, selectedIds, customAmts)
                }
                showAddExpenseDialog = false
                expenseToEdit = null
            }
        )
    }

    if (showRecordPaymentDialog) {
        val members by viewModel.activeGroupMembers.collectAsStateWithLifecycle()
        val activeGroup by viewModel.activeGroup.collectAsStateWithLifecycle()
        val groupCurrency = activeGroup?.currencySymbol ?: "$"
        RecordPaymentDialog(
            members = members,
            preFillSettlement = settleUpPreFill,
            currency = groupCurrency,
            onDismiss = {
                showRecordPaymentDialog = false
                settleUpPreFill = null
            },
            onSave = { fromId, toId, amount, status ->
                viewModel.addPayment(fromId, toId, amount, status)
                showRecordPaymentDialog = false
                settleUpPreFill = null
            }
        )
    }
}

// --- DRAWER LAYOUT SIDEBAR ---

@Composable
fun SidebarDrawerContent(
    groups: List<GroupWithStats>,
    selectedGroupId: Int?,
    currentUserEmail: String,
    currentUserName: String,
    preferredCurrency: String,
    onDashboardSelected: () -> Unit,
    onGroupSelected: (Int) -> Unit,
    onCreateGroupClick: () -> Unit,
    onRefreshDemoClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onUpdateProfile: (String, String, String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1C19))
            .padding(16.dp)
    ) {
        // Drawer Header with App Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_logo_1783531906531),
                    contentDescription = "SplitB Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .border(1.dp, Color(0xFF049469), RoundedCornerShape(9.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("SplitB", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("GROUP SPLITTER", color = Color(0xFF424940), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onCloseDrawer) {
                Icon(Icons.Default.Close, contentDescription = "Close drawer", tint = Color(0xFF424940))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Items Scroll Container
        Column(
            modifier = Modifier
                .weight(1.0f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Dashboard Option
            val isDashboardSelected = selectedGroupId == null
            NavigationItem(
                label = "Dashboard",
                icon = Icons.Default.Dashboard,
                isSelected = isDashboardSelected,
                onClick = onDashboardSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // My Groups Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF424940), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MY GROUPS",
                        color = Color(0xFF424940),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(
                    onClick = onCreateGroupClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Group", tint = Color(0xFF424940), modifier = Modifier.size(16.dp))
                }
            }

            // Group List
            groups.forEach { groupStats ->
                val isSelected = selectedGroupId == groupStats.group.id
                NavigationItem(
                    label = groupStats.group.name,
                    icon = Icons.Default.Folder,
                    isSelected = isSelected,
                    subtitle = "${groupStats.members.size} members",
                    onClick = { onGroupSelected(groupStats.group.id) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Demo Sandbox Control Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C19)),
                border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Demo Sandbox", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "Need more realistic data? You can reset the database and seed fresh groups.",
                        color = Color(0xFF72796F),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Button(
                        onClick = onRefreshDemoClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1C19)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Refresh Seed Data", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        var showProfileDialog by remember { mutableStateOf(false) }

        if (showProfileDialog) {
            ProfileSettingsDialog(
                currentName = currentUserName,
                currentEmail = currentUserEmail,
                currentCurrency = preferredCurrency,
                onDismiss = { showProfileDialog = false },
                onSave = onUpdateProfile
            )
        }

        // Profile Card at bottom (interactive)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showProfileDialog = true }
                .testTag("profile_card_settings"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2D2A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF386A20)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentUserName.isNotEmpty()) currentUserName.take(1).uppercase() else "Y",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1.0f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentUserName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            text = preferredCurrency,
                            color = Color(0xFF049469),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentUserEmail,
                            color = Color(0xFF72796F),
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Profile Settings",
                            tint = Color(0xFF72796F),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sign Out Button
        OutlinedButton(
            onClick = onSignOutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFBA1A1A)),
            border = BorderStroke(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.3f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun ProfileSettingsDialog(
    currentName: String,
    currentEmail: String,
    currentCurrency: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    var currency by remember { mutableStateOf(currentCurrency) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF386A20))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Profile Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1C19))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                // Avatar / Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF386A20))
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (name.isNotEmpty()) name.take(1).uppercase() else "Y",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                }

                // Name field
                Column {
                    Text("FULL NAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                        singleLine = true
                    )
                }

                // Email field
                Column {
                    Text("EMAIL ADDRESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                        singleLine = true
                    )
                }

                // Preferred Currency Selector
                Column {
                    Text("PREFERRED CURRENCY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(4.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val currencyOptions = listOf(
                        "$" to "US Dollar ($)",
                        "€" to "Euro (€)",
                        "£" to "British Pound (£)",
                        "¥" to "Yen / Yuan (¥)",
                        "₹" to "Indian Rupee (₹)",
                        "A$" to "Australian Dollar (A$)",
                        "C$" to "Canadian Dollar (C$)",
                        "S$" to "Singapore Dollar (S$)",
                        "₩" to "Korean Won (₩)",
                        "₽" to "Russian Ruble (₽)",
                        "₺" to "Turkish Lira (₺)",
                        "CHF" to "Swiss Franc (CHF)",
                        "kr" to "Krona (kr)",
                        "R$" to "Brazilian Real (R$)",
                        "R" to "South African Rand (R)",
                        "AED" to "UAE Dirham (AED)"
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFDDE5D9))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val selectedLabel = currencyOptions.find { it.first == currency }?.second ?: "US Dollar ($)"
                                Text(text = selectedLabel, color = Color(0xFF1A1C19))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF424940))
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            currencyOptions.forEach { (sym, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        currency = sym
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This currency symbol will be the default selected for any new group you create.",
                        color = Color(0xFF72796F),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF72796F))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(name.trim(), email.trim(), currency)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386A20))
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp)),
        color = if (isSelected) Color(0xFF386A20) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF72796F),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color(0xFFDDE5D9),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color(0xFF424940),
                        fontSize = 10.sp
                    )
                }
            }
            if (!isSelected && subtitle == null) {
                Spacer(modifier = Modifier.weight(1.0f))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF424940),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// --- DASHBOARD SCREEN CONTENT ---

@Composable
fun DashboardScreen(
    viewModel: SplitViewModel,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (Int) -> Unit
) {
    val stats by viewModel.dashboardOverviewStats.collectAsStateWithLifecycle()
    val groups by viewModel.groupsWithStats.collectAsStateWithLifecycle()
    val activities by viewModel.recentActivities.collectAsStateWithLifecycle()
    val preferredCurrency by viewModel.preferredCurrency.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Headline
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Overview Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C19)
                )
                Text(
                    text = "Track and manage your shared budgets and group activities.",
                    fontSize = 14.sp,
                    color = Color(0xFF424940)
                )
            }
        }

        // Create New Group CTA Button
        item {
            Button(
                onClick = onCreateGroupClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_group_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F39F6)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Group", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Summary Balance Cards (TOTAL BALANCE, YOU ARE OWED, YOU OWE)
        item {
            NaturalTonesBalanceCard(
                totalBalance = stats.first,
                youAreOwed = stats.second,
                youOwe = stats.third,
                currency = preferredCurrency
            )
        }

        // Active Groups Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Active Groups (${groups.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C19)
                )
            }
        }

        if (groups.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No active groups yet",
                    description = "Create a group to start splitting bills and tracking payments with friends or roommates.",
                    actionText = "Create Group",
                    onAction = onCreateGroupClick
                )
            }
        } else {
            items(groups) { groupWithStats ->
                GroupCard(
                    groupWithStats = groupWithStats,
                    onClick = { onGroupClick(groupWithStats.group.id) }
                )
            }
        }

        // Recent Activities Timeline
        item {
            Text(
                text = "Recent Activities",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C19),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (activities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent activity found.", color = Color(0xFF424940), fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(activities.take(15)) { activity ->
                ActivityRow(
                    activity = activity,
                    onClickGroup = { onGroupClick(activity.groupId) }
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtext: String,
    color: Color,
    containerColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424940),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtext,
                    fontSize = 11.sp,
                    color = Color(0xFF424940)
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun NaturalTonesBalanceCard(
    totalBalance: Double,
    youAreOwed: Double,
    youOwe: Double,
    currency: String = "$"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDDE5D9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "TOTAL BALANCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424940),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = (if (totalBalance >= 0) "+" else "") + String.format("%s%.2f", currency, totalBalance),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1A1C19)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFBFCBAD), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = Color(0xFF386A20),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // You Owe
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF1F5EB), shape = RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "YOU OWE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424940).copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%s%.2f", currency, youOwe),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBA1A1A)
                    )
                }

                // Owed to You
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF1F5EB), shape = RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "OWED TO YOU",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424940).copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%s%.2f", currency, youAreOwed),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF386A20)
                    )
                }
            }
        }
    }
}

fun getGroupIconAndColor(name: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, Pair<Color, Color>> {
    val lower = name.lowercase()
    return when {
        lower.contains("trip") or lower.contains("travel") or lower.contains("vacation") or lower.contains("flight") or lower.contains("iceland") -> {
            Icons.Default.Flight to (Color(0xFFE8F5E9) to Color(0xFF2E7D32))
        }
        lower.contains("home") or lower.contains("apartment") or lower.contains("flat") or lower.contains("roommate") or lower.contains("rent") or lower.contains("4b") -> {
            Icons.Default.Home to (Color(0xFFFFF3E0) to Color(0xFFE65100))
        }
        lower.contains("food") or lower.contains("dinner") or lower.contains("drinks") or lower.contains("restaurant") or lower.contains("cafe") or lower.contains("bar") -> {
            Icons.Default.Restaurant to (Color(0xFFF3E5F5) to Color(0xFF7B1FA2))
        }
        else -> {
            Icons.Default.GroupWork to (Color(0xFFE1F5FE) to Color(0xFF0288D1))
        }
    }
}

@Composable
fun GroupCard(
    groupWithStats: GroupWithStats,
    onClick: () -> Unit
) {
    val balance = groupWithStats.netBalanceForYou
    val (icon, colors) = getGroupIconAndColor(groupWithStats.group.name)
    val (iconBg, iconTint) = colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFDDE5D9)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Icon left
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Group Info middle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = groupWithStats.group.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C19)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${groupWithStats.members.size} members • ${groupWithStats.billCount} bills",
                    fontSize = 12.sp,
                    color = Color(0xFF72796F)
                )
            }

            // Balance right
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val labelText = when {
                    balance > 0.01 -> "You are owed"
                    balance < -0.01 -> "You owe"
                    else -> "Settled"
                }
                val valueColor = when {
                    balance > 0.01 -> Color(0xFF386A20)
                    balance < -0.01 -> Color(0xFFBA1A1A)
                    else -> Color(0xFF1A1C19).copy(alpha = 0.3f)
                }
                Text(
                    text = labelText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (balance > 0.01) Color(0xFF386A20) else if (balance < -0.01) Color(0xFFBA1A1A) else Color(0xFF72796F)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format("%s%.2f", groupWithStats.group.currencySymbol, kotlin.math.abs(balance)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            }
        }
    }
}

@Composable
fun ActivityRow(
    activity: ActivityItem,
    onClickGroup: () -> Unit
) {
    val dateStr = SimpleDateFormat("d MMM, hh:mm", Locale.getDefault()).format(Date(activity.dateMillis))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClickGroup)
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFF1F5EB), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Circle
        val iconColor = if (activity.isPayment) Color(0xFF386A20) else Color(0xFF386A20)
        val iconBg = if (activity.isPayment) Color(0xFFD7E8CD) else Color(0xFFF1F5EB)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = activity.icon,
                color = iconColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = activity.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1C19)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = activity.subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF386A20),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "•  $dateStr",
                    fontSize = 11.sp,
                    color = Color(0xFF72796F)
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = String.format("%s%.2f", activity.currencySymbol, activity.amount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (activity.isPayment) Color(0xFF386A20) else Color(0xFF1A1C19)
            )
            if (activity.statusText.isNotEmpty()) {
                Text(
                    text = activity.statusText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
        }
    }
}

// --- GROUP DETAILS VIEW SCREEN ---

@Composable
fun GroupDetailScreen(
    viewModel: SplitViewModel,
    onBackToDashboard: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    onSettleUpClick: (SimplifiedSettlement) -> Unit,
    onEditExpenseClick: (ExpenseWithDetails) -> Unit
) {
    val activeGroup by viewModel.activeGroup.collectAsStateWithLifecycle()
    val members by viewModel.activeGroupMembers.collectAsStateWithLifecycle()
    val expenses by viewModel.activeGroupExpenses.collectAsStateWithLifecycle()
    val payments by viewModel.activeGroupPayments.collectAsStateWithLifecycle()
    val netBalances by viewModel.activeGroupNetBalances.collectAsStateWithLifecycle()
    val settlements by viewModel.activeGroupSettlements.collectAsStateWithLifecycle()
    val recentActivities by viewModel.recentActivities.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    var showRenameDialog by remember { mutableStateOf(false) }

    if (activeGroup == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val group = activeGroup!!
    val memberNames = members.joinToString(", ") { if (it.isYou) "You" else it.name }
    val totalGroupExpense = expenses.sumOf { it.expense.amount }
    val myBalance = netBalances.find { it.member.isYou }
    val myShare = myBalance?.share ?: 0.0
    val youOwe = settlements.filter { it.fromMember.isYou }.sumOf { it.amount }
    val owedToYou = settlements.filter { it.toMember.isYou }.sumOf { it.amount }
    val currency = group.currencySymbol

    val listState = rememberLazyListState()
    LaunchedEffect(selectedTab) {
        listState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Back To Dashboard CTA
        TextButton(
            onClick = onBackToDashboard,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("BACK TO DASHBOARD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Group Summary Card with actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFF1F5EB))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = group.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1A1C19)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showRenameDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename group", tint = Color(0xFF72796F), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF72796F), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${members.size} Members:  $memberNames",
                        fontSize = 13.sp,
                        color = Color(0xFF424940),
                        maxLines = 2
                    )
                }

                // Financial Summary Tiles at Group level
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("group_financial_summary_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAF7)),
                    border = BorderStroke(1.dp, Color(0xFFEBEFE6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "GROUP FINANCIAL SUMMARY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5B6256),
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Group Expense Box
                            Card(
                                modifier = Modifier.weight(1f).testTag("group_total_expense"),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFEBEFE6)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = "Total Group",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF72796F),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format("%s%.2f", currency, totalGroupExpense),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1C19),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Your Expense Share Box
                            Card(
                                modifier = Modifier.weight(1f).testTag("your_share_expense"),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFEBEFE6)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = "Your Share",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF72796F),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format("%s%.2f", currency, myShare),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1C19),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // You Owe Box
                            Card(
                                modifier = Modifier.weight(1f).testTag("you_owe_expense"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (youOwe > 0.01) Color(0xFFFFF0EE) else Color.White
                                ),
                                border = BorderStroke(1.dp, if (youOwe > 0.01) Color(0xFFF9DEDC) else Color(0xFFEBEFE6)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = "You Owe",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (youOwe > 0.01) Color(0xFFBA1A1A) else Color(0xFF72796F),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format("%s%.2f", currency, youOwe),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (youOwe > 0.01) Color(0xFFBA1A1A) else Color(0xFF1A1C19),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Owed to You Box
                            Card(
                                modifier = Modifier.weight(1f).testTag("owed_to_you_expense"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (owedToYou > 0.01) Color(0xFFE8F5E9) else Color.White
                                ),
                                border = BorderStroke(1.dp, if (owedToYou > 0.01) Color(0xFFC8E6C9) else Color(0xFFEBEFE6)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = "Owed to You",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (owedToYou > 0.01) Color(0xFF386A20) else Color(0xFF72796F),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format("%s%.2f", currency, owedToYou),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (owedToYou > 0.01) Color(0xFF386A20) else Color(0xFF1A1C19),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRecordPaymentClick,
                        modifier = Modifier
                            .weight(1.0f)
                            .height(44.dp)
                            .testTag("record_payment_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F39F6)),
                        border = BorderStroke(1.dp, Color(0xFF4F39F6)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record Payment", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    }

                    Button(
                        onClick = onAddExpenseClick,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("add_expense_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F39F6)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Expense", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }

        stickyHeader {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = { HorizontalDivider(color = Color(0xFFDDE5D9)) },
                    edgePadding = 0.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Expenses (${expenses.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Balances & Settlements", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Payment Log (${payments.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Activity Log", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // EXPENSES TAB
                if (expenses.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No expenses recorded",
                            description = "Add some grocery, dinner, or travel bills to split among your group members.",
                            actionText = "Add Expense",
                            onAction = onAddExpenseClick
                        )
                    }
                } else {
                    items(expenses) { expenseWithDetails ->
                        ExpenseDetailRow(
                            expenseWithDetails = expenseWithDetails,
                            currency = group.currencySymbol,
                            onEdit = { onEditExpenseClick(expenseWithDetails) },
                            onDelete = { viewModel.deleteExpense(expenseWithDetails.expense.id) }
                        )
                    }
                }
            }
            1 -> {
                // BALANCES & SETTLEMENTS TAB
                item {
                    Text(
                        text = "Member Net Balances",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1C19),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(netBalances) { netBalance ->
                    MemberNetBalanceRow(
                        netBalance = netBalance,
                        currency = group.currencySymbol
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Simplified Net Settlements",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1C19)
                    )
                }

                // Settlement explanatory box
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5EB)),
                        border = BorderStroke(1.dp, Color(0xFFD7E8CD))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF386A20), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "We've simplified your transactions. Rather than everyone paying everyone back individually, these calculated transfers will settle all debts optimally.",
                                color = Color(0xFF1A1C19),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                if (settlements.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD7E8CD))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF386A20))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "All balances are perfectly settled up!",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF386A20),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(settlements) { settlement ->
                        SettlementRow(
                            settlement = settlement,
                            currency = group.currencySymbol,
                            onSettleUpClick = { onSettleUpClick(settlement) }
                        )
                    }
                }
            }
            2 -> {
                // PAYMENT LOG TAB
                if (payments.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No payments logged yet",
                            description = "Record a settlement payment when someone transfers cash or pays through banking to clear their balances.",
                            actionText = "Record Payment",
                            onAction = onRecordPaymentClick
                        )
                    }
                } else {
                    items(payments) { paymentWithDetails ->
                        PaymentLogCard(
                            paymentWithDetails = paymentWithDetails,
                            currency = group.currencySymbol,
                            onApprove = { viewModel.approvePayment(paymentWithDetails.payment.id) },
                            onRemind = { viewModel.remindPayment(paymentWithDetails.payment.id) },
                            onDelete = { viewModel.deletePayment(paymentWithDetails.payment.id) }
                        )
                    }
                }
            }
            3 -> {
                // ACTIVITY LOG TIMELINE TAB
                val groupActivities = recentActivities.filter { it.groupId == group.id }
                if (groupActivities.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No activity recorded for this group.", color = Color(0xFF424940), fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5EB))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF386A20))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Group History Timeline", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1C19))
                                }

                                groupActivities.forEachIndexed { index, activity ->
                                    val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(activity.dateMillis))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Bullet Point and vertical line
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(top = 4.dp, end = 12.dp)
                                        ) {
                                            val bColor = if (activity.isPayment) Color(0xFF386A20) else Color(0xFF386A20)
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(bColor)
                                            )
                                            if (index < groupActivities.size - 1) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(1.dp)
                                                        .height(50.dp)
                                                        .background(Color(0xFFDDE5D9))
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = activity.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1A1C19)
                                            )
                                            Text(
                                                text = dateFmt,
                                                fontSize = 10.sp,
                                                color = Color(0xFF72796F)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (activity.isPayment) {
                                                    "${activity.fromName} paid ${if (activity.toName == "You (You (Demo))") "You" else activity.toName}"
                                                } else {
                                                    "Split with " + members.joinToString(", ") { if (it.isYou) "You" else it.name }
                                                },
                                                fontSize = 12.sp,
                                                color = Color(0xFF424940)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFF1F5EB))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = String.format("%s%.2f", group.currencySymbol, activity.amount),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1A1C19)
                                                    )
                                                }
                                                if (activity.statusText.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFFFF3E0))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "PENDING",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFE65100)
                                                        )
                                                    }
                                                } else if (activity.isPayment) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFD7E8CD))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "COMPLETED",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF386A20)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete group button at very bottom
        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { viewModel.deleteGroup(group.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFBA1A1A)),
                border = BorderStroke(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("DELETE THIS GROUP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(group.name) }
        Dialog(onDismissRequest = { showRenameDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Rename Group", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1C19))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showRenameDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    viewModel.renameGroup(group, newName)
                                }
                                showRenameDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F39F6))
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseDetailRow(
    expenseWithDetails: ExpenseWithDetails,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val exp = expenseWithDetails.expense
    val dateStr = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(exp.date))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFF1F5EB), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5EB)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF386A20))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = exp.description,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C19)
            )
            Text(
                text = expenseWithDetails.detailText,
                fontSize = 11.sp,
                color = Color(0xFF424940)
            )
            Text(
                text = dateStr,
                fontSize = 10.sp,
                color = Color(0xFF72796F)
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = String.format("Total Cost:  %s%.2f", currency, exp.amount),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424940)
            )

            // Lending state color
            if (expenseWithDetails.lentAmount > 0.01) {
                Text(
                    text = String.format("You lent %s%.2f", currency, expenseWithDetails.lentAmount),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF386A20)
                )
            } else if (expenseWithDetails.oweAmount > 0.01) {
                Text(
                    text = String.format("You owe %s%.2f", currency, expenseWithDetails.oweAmount),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFBA1A1A)
                )
            }

            // Edit / Delete icons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF72796F), modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFBA1A1A).copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun MemberNetBalanceRow(
    netBalance: MemberNetBalance,
    currency: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFF1F5EB), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Initial
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFDDE5D9)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = netBalance.member.name.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF424940)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = if (netBalance.member.isYou) "${netBalance.member.name} (You)" else netBalance.member.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C19)
            )
            Text(
                text = String.format("Spent: %s%.2f  •  Share: %s%.2f", currency, netBalance.spent, currency, netBalance.share),
                fontSize = 11.sp,
                color = Color(0xFF424940)
            )
        }

        // Net Balance State
        val text = when {
            netBalance.net > 0.01 -> String.format("is owed %s%.2f", currency, netBalance.net)
            netBalance.net < -0.01 -> String.format("owes %s%.2f", currency, -netBalance.net)
            else -> "Settled up"
        }
        val textColor = when {
            netBalance.net > 0.01 -> Color(0xFF386A20)
            netBalance.net < -0.01 -> Color(0xFFBA1A1A)
            else -> Color(0xFF424940)
        }

        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun SettlementRow(
    settlement: SimplifiedSettlement,
    currency: String,
    onSettleUpClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFF1F5EB), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.0f)
        ) {
            Text(
                text = if (settlement.fromMember.isYou) "You" else settlement.fromMember.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C19)
            )
            
            // Settlement amount arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = String.format("%s%.2f", currency, settlement.amount),
                    fontSize = 11.sp,
                    color = Color(0xFF424940),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF72796F),
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = if (settlement.toMember.isYou) "You" else settlement.toMember.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C19)
            )
        }

        Button(
            onClick = onSettleUpClick,
            modifier = Modifier
                .height(34.dp)
                .testTag("settle_button_${settlement.fromMember.id}"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5EB), contentColor = Color(0xFF4F39F6)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("Settle up", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PaymentLogCard(
    paymentWithDetails: PaymentWithDetails,
    currency: String,
    onApprove: () -> Unit,
    onRemind: () -> Unit,
    onDelete: () -> Unit
) {
    val p = paymentWithDetails.payment
    val dateStr = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(p.date))
    val isPending = p.status == "PENDING_APPROVAL"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5EB))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPending) Color(0xFFFFF3E0) else Color(0xFFD7E8CD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPending) Icons.Default.PendingActions else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isPending) Color(0xFFE65100) else Color(0xFF386A20)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = "${if (paymentWithDetails.fromMember.isYou) "You" else paymentWithDetails.fromMember.name} paid ${if (paymentWithDetails.toMember.isYou) "You" else paymentWithDetails.toMember.name}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1C19)
                    )
                    Text(
                        text = "Settlement payment of $currency${String.format("%.2f", p.amount)}",
                        fontSize = 12.sp,
                        color = Color(0xFF424940)
                    )
                    Text(
                        text = "$dateStr",
                        fontSize = 10.sp,
                        color = Color(0xFF72796F)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete payment", tint = Color(0xFFBA1A1A).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }

            // Pending approval state buttons
            if (isPending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "*Recipient must approve to clear from balances.",
                        fontSize = 10.sp,
                        color = Color(0xFF424940),
                        modifier = Modifier.weight(1.0f)
                    )

                    OutlinedButton(
                        onClick = onRemind,
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Only the recipient "You" should realistically approve, but let anyone approve for demo flexibility
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F39F6)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Approve ✔", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFD7E8CD))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "COMPLETED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF386A20)
                    )
                }
            }
        }
    }
}

// --- COMMONLY USED HELPER LAYOUTS ---

@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5EB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF72796F), modifier = Modifier.size(24.dp))
            }
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1A1C19)
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFF424940),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF4F39F6)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F39F6))
            ) {
                Text(actionText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- POPUP FORMS / DIALOG DESIGNS ---

@Composable
fun CreateGroupDialog(
    defaultCurrency: String = "$",
    onDismiss: () -> Unit,
    onCreate: (String, String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var currentMemberInput by remember { mutableStateOf("") }
    val membersList = remember { mutableStateListOf<String>() }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight - 64.dp)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color(0xFF386A20))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create a New Group", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1C19))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                // Group Name
                Column {
                    Text("GROUP NAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Roommates 2B, Kyoto Autumn Trip") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input"),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                        singleLine = true
                    )
                }

                // Description
                Column {
                    Text("DESCRIPTION (OPTIONAL)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Describe group budgets, members, or purpose...") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                        maxLines = 3
                    )
                }

                // Currency Dropdown
                Column {
                    Text("CURRENCY SYMBOL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(4.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val currencyOptions = listOf(
                        "$" to "US Dollar ($)",
                        "€" to "Euro (€)",
                        "£" to "British Pound (£)",
                        "¥" to "Yen / Yuan (¥)",
                        "₹" to "Indian Rupee (₹)",
                        "A$" to "Australian Dollar (A$)",
                        "C$" to "Canadian Dollar (C$)",
                        "S$" to "Singapore Dollar (S$)",
                        "₩" to "Korean Won (₩)",
                        "₽" to "Russian Ruble (₽)",
                        "₺" to "Turkish Lira (₺)",
                        "CHF" to "Swiss Franc (CHF)",
                        "kr" to "Krona (kr)",
                        "R$" to "Brazilian Real (R$)",
                        "R" to "South African Rand (R)",
                        "AED" to "UAE Dirham (AED)"
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFDDE5D9))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val selectedLabel = currencyOptions.find { it.first == currency }?.second ?: "US Dollar ($)"
                                Text(text = selectedLabel, color = Color(0xFF1A1C19))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF424940))
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.heightIn(max = 250.dp)
                        ) {
                            currencyOptions.forEach { (sym, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        currency = sym
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Add Members section
                Column {
                    Text("ADD GROUP MEMBERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currentMemberInput,
                            onValueChange = { currentMemberInput = it },
                            placeholder = { Text("e.g. Alice, Bob, Charlie") },
                            modifier = Modifier
                                .weight(1.0f)
                                .testTag("member_name_input"),
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (currentMemberInput.isNotBlank()) {
                                    membersList.add(currentMemberInput.trim())
                                    currentMemberInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F39F6)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Members List Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                        border = BorderStroke(1.dp, Color(0xFFDDE5D9))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // "You" is always there
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("You (You (Demo))", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1A1C19))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFDDE5D9))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Creator", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                                }
                            }

                            membersList.forEachIndexed { idx, name ->
                                HorizontalDivider(color = Color(0xFFDDE5D9))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, fontSize = 13.sp, color = Color(0xFF1A1C19))
                                    IconButton(
                                        onClick = { membersList.removeAt(idx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircle, contentDescription = "Remove member", tint = Color(0xFFBA1A1A).copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1.0f)
                            .height(44.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onCreate(name.trim(), description.trim(), currency, membersList.toList())
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("submit_create_group"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F39F6)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Create Group", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditExpenseDialog(
    expenseToEdit: ExpenseWithDetails?,
    members: List<MemberEntity>,
    currency: String = "$",
    onDismiss: () -> Unit,
    onSave: (
        description: String,
        amount: Double,
        date: Long,
        paidByMemberId: Int,
        splitMethod: String,
        selectedMemberIds: List<Int>,
        customAmounts: Map<Int, Double>
    ) -> Unit
) {
    var description by remember { mutableStateOf(expenseToEdit?.expense?.description ?: "") }
    var amountStr by remember { mutableStateOf(expenseToEdit?.expense?.amount?.toString() ?: "") }
    var paidByMemberId by remember { mutableStateOf(expenseToEdit?.expense?.paidByMemberId ?: (members.find { it.isYou }?.id ?: 0)) }
    var splitMethod by remember { mutableStateOf(expenseToEdit?.expense?.splitMethod ?: "EQUAL") }
    var dateMillis by remember { mutableStateOf(expenseToEdit?.expense?.date ?: System.currentTimeMillis()) }

    val context = LocalContext.current
    val showDatePicker = {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                dateMillis = selectedCalendar.timeInMillis
            },
            year,
            month,
            day
        ).show()
    }

    // Multi-selection map for split participants
    val selectedMemberIds = remember {
        val list = mutableStateListOf<Int>()
        if (expenseToEdit != null) {
            expenseToEdit.splits.filter { it.split.isSelected }.forEach { list.add(it.member.id) }
        } else {
            members.forEach { list.add(it.id) } // Default select everyone
        }
        list
    }

    // Custom amounts text-fields state
    val customAmounts = remember {
        val map = mutableStateMapOf<Int, String>()
        if (expenseToEdit != null) {
            expenseToEdit.splits.forEach { map[it.member.id] = it.split.amount.toString() }
        } else {
            members.forEach { map[it.id] = "" }
        }
        map
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight - 64.dp)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddCard, contentDescription = null, tint = Color(0xFF386A20))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (expenseToEdit == null) "Add New Expense" else "Edit Expense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A1C19)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                // Description Input
                Column {
                    Text("EXPENSE DESCRIPTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("e.g. Dinner, Rent, WiFi") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_description_input"),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                        singleLine = true
                    )
                }

                // Amount & Date row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text("AMOUNT ($currency)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            placeholder = { Text("0.00") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_amount_input"),
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Column(modifier = Modifier.weight(1.0f)) {
                        Text("DATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_date_container")
                                .clickable { showDatePicker() }
                        ) {
                            OutlinedTextField(
                                value = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(dateMillis)),
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = "Select Date",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF4F39F6)
                                    )
                                },
                                readOnly = true,
                                singleLine = true
                            )
                            // Transparent overlay to safely intercept all click events
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .testTag("expense_date_input")
                                    .clickable { showDatePicker() }
                            )
                        }
                    }
                }

                // Who Paid Dropdown
                Column {
                    Text("WHO PAID?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(4.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val selectedPaidBy = members.find { it.id == paidByMemberId } ?: members.firstOrNull()

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFDDE5D9))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedPaidBy?.isYou == true) "You" else (selectedPaidBy?.name ?: "Select"),
                                    color = Color(0xFF1A1C19)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF424940))
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            members.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(if (m.isYou) "You" else m.name) },
                                    onClick = {
                                        paidByMemberId = m.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Split Method Switcher
                Column {
                    Text("SPLIT METHOD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5EB))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { splitMethod = "EQUAL" },
                            modifier = Modifier.weight(1.0f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (splitMethod == "EQUAL") Color.White else Color.Transparent,
                                contentColor = if (splitMethod == "EQUAL") Color(0xFF4F39F6) else Color(0xFF424940)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Split Equally", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { splitMethod = "CUSTOM" },
                            modifier = Modifier.weight(1.0f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (splitMethod == "CUSTOM") Color.White else Color.Transparent,
                                contentColor = if (splitMethod == "CUSTOM") Color(0xFF4F39F6) else Color(0xFF424940)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Custom Split", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Split With Checklist Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SPLIT WITH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                        Row {
                            Text(
                                text = "Select All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF386A20),
                                modifier = Modifier
                                    .clickable {
                                        selectedMemberIds.clear()
                                        members.forEach { selectedMemberIds.add(it.id) }
                                    }
                                    .padding(horizontal = 6.dp)
                            )
                            VerticalDivider(modifier = Modifier.height(14.dp), color = Color(0xFFDDE5D9))
                            Text(
                                text = "Only Me",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF386A20),
                                modifier = Modifier
                                    .clickable {
                                        selectedMemberIds.clear()
                                        val youId = members.find { it.isYou }?.id
                                        if (youId != null) selectedMemberIds.add(youId)
                                    }
                                    .padding(horizontal = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Split list Card container
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                        border = BorderStroke(1.dp, Color(0xFFDDE5D9))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            members.forEach { m ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.0f)) {
                                        Checkbox(
                                            checked = selectedMemberIds.contains(m.id),
                                            onCheckedChange = { chk ->
                                                if (chk == true) {
                                                    selectedMemberIds.add(m.id)
                                                } else {
                                                    selectedMemberIds.remove(m.id)
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = if (m.isYou) "You" else m.name, fontSize = 13.sp, color = Color(0xFF1A1C19))
                                    }

                                    if (splitMethod == "EQUAL") {
                                        // Display split fraction share
                                        val totalSelected = selectedMemberIds.size
                                        val totalAmt = amountStr.toDoubleOrNull() ?: 0.0
                                        val shareVal = if (totalSelected > 0 && selectedMemberIds.contains(m.id)) totalAmt / totalSelected else 0.0
                                        Text(
                                            text = String.format("%s%.2f", currency, shareVal),
                                            color = Color(0xFF72796F),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        // Display custom input box
                                        var mVal = customAmounts[m.id] ?: ""
                                        OutlinedTextField(
                                            value = mVal,
                                            onValueChange = { customAmounts[m.id] = it },
                                            modifier = Modifier
                                                .width(110.dp)
                                                .height(50.dp)
                                                .testTag("custom_amt_${m.id}"),
                                            placeholder = { Text("0.00", fontSize = 12.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF049469)),
                                            singleLine = true,
                                            enabled = selectedMemberIds.contains(m.id)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Real-time Split Calculator & Validator
                val totalSpent = amountStr.toDoubleOrNull() ?: 0.0
                if (splitMethod == "CUSTOM") {
                    val customAmtsSum = selectedMemberIds.sumOf { mId ->
                        customAmounts[mId]?.toDoubleOrNull() ?: 0.0
                    }
                    val diff = totalSpent - customAmtsSum
                    val isMatched = diff >= -0.001 && diff <= 0.001

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isMatched -> Color(0xFFE8F5E9) // Light green background
                                    diff > 0 -> Color(0xFFFFF3E0) // Light orange background (short of spent)
                                    else -> Color(0xFFFFEBEE) // Light red background (over spent)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isMatched -> Color(0xFF81C784)
                                    diff > 0 -> Color(0xFFFFB74D)
                                    else -> Color(0xFFE57373)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Split Calculator",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF424940)
                                )
                                Text(
                                    text = if (isMatched) "✓ Split Balanced" else "⚠ Split Mismatch",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isMatched) Color(0xFF2E7D32) else if (diff > 0) Color(0xFFE65100) else Color(0xFFC62828)
                                )
                            }
                            
                            HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Spent Amount:", fontSize = 12.sp, color = Color(0xFF424940))
                                Text(String.format(Locale.US, "%s%.2f", currency, totalSpent), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1C19))
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sum of Custom Splits:", fontSize = 12.sp, color = Color(0xFF424940))
                                Text(String.format(Locale.US, "%s%.2f", currency, customAmtsSum), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1C19))
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Status / Difference:", fontSize = 12.sp, color = Color(0xFF424940))
                                val diffText = when {
                                    isMatched -> "Matches perfectly!"
                                    diff > 0 -> String.format(Locale.US, "Short by %s%.2f", currency, diff)
                                    else -> String.format(Locale.US, "Over by %s%.2f", currency, -diff)
                                }
                                Text(
                                    text = diffText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMatched) Color(0xFF2E7D32) else if (diff > 0) Color(0xFFE65100) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1.0f)
                            .height(44.dp)
                    ) {
                        Text("Cancel")
                    }

                    val customAmtsSum = selectedMemberIds.sumOf { customAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                    val isCustomSplitValid = splitMethod != "CUSTOM" || (totalSpent > 0.0 && (totalSpent - customAmtsSum) >= -0.001 && (totalSpent - customAmtsSum) <= 0.001)

                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            val customAmtsConverted = customAmounts.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                            onSave(
                                description.trim(),
                                amt,
                                dateMillis,
                                paidByMemberId,
                                splitMethod,
                                selectedMemberIds.toList(),
                                customAmtsConverted
                            )
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("submit_expense_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F39F6)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = description.isNotBlank() && amountStr.toDoubleOrNull() != null && isCustomSplitValid
                    ) {
                        Text(if (expenseToEdit == null) "Add Expense" else "Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(
    members: List<MemberEntity>,
    preFillSettlement: SimplifiedSettlement?,
    currency: String = "$",
    onDismiss: () -> Unit,
    onSave: (fromId: Int, toId: Int, amount: Double, status: String) -> Unit
) {
    var fromId by remember { mutableStateOf(preFillSettlement?.fromMember?.id ?: (members.firstOrNull()?.id ?: 0)) }
    var toId by remember { mutableStateOf(preFillSettlement?.toMember?.id ?: (members.firstOrNull { it.isYou }?.id ?: (members.getOrNull(1)?.id ?: 0))) }
    var amountStr by remember { mutableStateOf(preFillSettlement?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "0.00") }
    var status by remember { mutableStateOf("COMPLETED") } // COMPLETED or PENDING_APPROVAL

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight - 64.dp)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF386A20))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Record Settlement Payment", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1C19))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                // From & To Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // From
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text("FROM (WHO SENT)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                        Spacer(modifier = Modifier.height(4.dp))
                        var expandedFrom by remember { mutableStateOf(false) }
                        val fromMember = members.find { it.id == fromId }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedFrom = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFDDE5D9)),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = if (fromMember?.isYou == true) "You" else (fromMember?.name ?: "Select"), color = Color(0xFF1A1C19), maxLines = 1)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF424940))
                                }
                            }
                            DropdownMenu(
                                expanded = expandedFrom,
                                onDismissRequest = { expandedFrom = false }
                            ) {
                                members.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(if (m.isYou) "You" else m.name) },
                                        onClick = {
                                            fromId = m.id
                                            expandedFrom = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // To
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text("TO (WHO RECEIVED)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                        Spacer(modifier = Modifier.height(4.dp))
                        var expandedTo by remember { mutableStateOf(false) }
                        val toMember = members.find { it.id == toId }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedTo = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFDDE5D9)),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = if (toMember?.isYou == true) "You" else (toMember?.name ?: "Select"), color = Color(0xFF1A1C19), maxLines = 1)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF424940))
                                }
                            }
                            DropdownMenu(
                                expanded = expandedTo,
                                onDismissRequest = { expandedTo = false }
                            ) {
                                members.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(if (m.isYou) "You" else m.name) },
                                        onClick = {
                                            toId = m.id
                                            expandedTo = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Settlement Amount
                Column {
                    Text("SETTLEMENT AMOUNT ($currency)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settlement_amount_input"),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF049469)),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Settlement Status switcher
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SETTLEMENT STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424940))
                    Spacer(modifier = Modifier.height(2.dp))

                    // Completed button
                    OutlinedButton(
                        onClick = { status = "COMPLETED" },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = if (status == "COMPLETED") 2.dp else 1.dp,
                            color = if (status == "COMPLETED") Color(0xFF386A20) else Color(0xFFDDE5D9)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (status == "COMPLETED") Color(0xFFD7E8CD) else Color.Transparent,
                            contentColor = if (status == "COMPLETED") Color(0xFF386A20) else Color(0xFF424940)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Completed", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Pending Approval button
                    OutlinedButton(
                        onClick = { status = "PENDING_APPROVAL" },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = if (status == "PENDING_APPROVAL") 2.dp else 1.dp,
                            color = if (status == "PENDING_APPROVAL") Color(0xFFE65100) else Color(0xFFDDE5D9)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (status == "PENDING_APPROVAL") Color(0xFFFFF3E0) else Color.Transparent,
                            contentColor = if (status == "PENDING_APPROVAL") Color(0xFFE65100) else Color(0xFF424940)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pending Approval", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "*Completed settlements immediately clear balances. Pending settlements can be verified and reminded.",
                        fontSize = 11.sp,
                        color = Color(0xFF424940),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions Column
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (fromId != toId && amt > 0.0) {
                                onSave(fromId, toId, amt, status)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_payment_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F39F6)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = fromId != toId && amountStr.toDoubleOrNull() != null && (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
                    ) {
                        Text("Save Settlement", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFDDE5D9))
                    ) {
                        Text("Cancel", color = Color(0xFF424940), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
