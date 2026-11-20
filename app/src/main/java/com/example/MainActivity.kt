package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.signOut
import com.example.data.TrackerRepository
import com.example.model.ProgressMetrics
import com.example.model.Supplement
import com.example.model.SupplementStatus
import com.example.model.University
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EssayTypesScreen
import com.example.ui.screens.SchoolEditorSheet
import com.example.ui.screens.SchoolsScreen
import com.example.ui.screens.SupplementEditorSheet
import com.example.ui.screens.UniversityDetailScreen
import com.example.ui.state.TrackerSnapshot
import com.example.ui.state.TrackerViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.CollegeTrackerTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_SCHOOLS = "schools"
private const val ROUTE_CALENDAR = "calendar"
private const val ROUTE_ESSAY_TYPES = "essay_types"
private const val ROUTE_UNIVERSITY_DETAIL = "university_detail/{universityId}"

private fun universityDetailRoute(universityId: String) = "university_detail/$universityId"

private enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(
        route = ROUTE_DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    SCHOOLS(
        route = ROUTE_SCHOOLS,
        labelRes = R.string.nav_schools,
        selectedIcon = Icons.Filled.AccountBalance,
        unselectedIcon = Icons.Outlined.AccountBalance,
        testTag = "nav_schools"
    ),
    CALENDAR(
        route = ROUTE_CALENDAR,
        labelRes = R.string.nav_calendar,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        testTag = "nav_calendar"
    ),
    ESSAY_TYPES(
        route = ROUTE_ESSAY_TYPES,
        labelRes = R.string.nav_essay_types,
        selectedIcon = Icons.Filled.Category,
        unselectedIcon = Icons.Outlined.Category,
        testTag = "nav_essay_types"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CollegeTrackerTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            AuthenticatedTrackerApp(currentUser = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedTrackerApp(currentUser: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: TrackerViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                TrackerViewModel(TrackerRepository(db), currentUser.uid)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelRoute = TopLevelDestination.entries.any { it.route == currentRoute }

    var showSchoolEditor by remember { mutableStateOf(false) }
    var editingUniversity by remember { mutableStateOf<University?>(null) }

    var supplementSheetState by remember { mutableStateOf<Pair<University, Supplement?>?>(null) }
    var showAccountMenu by remember { mutableStateOf(false) }

    val openAddSchool: () -> Unit = {
        editingUniversity = null
        showSchoolEditor = true
    }

    val openEditSchool: (University) -> Unit = { uni ->
        editingUniversity = uni
        showSchoolEditor = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && isTopLevelRoute) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.size(16.dp))
                    TopLevelDestination.entries.forEach { dest ->
                        val selected = currentRoute == dest.route
                        val label = stringResource(dest.labelRes)
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(dest.route) {
                                        popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (isTopLevelRoute) {
                        val titleText = when (currentRoute) {
                            ROUTE_SCHOOLS -> "Target Schools"
                            ROUTE_CALENDAR -> "Deadline Calendar"
                            ROUTE_ESSAY_TYPES -> "Essay Types"
                            else -> "College Tracker"
                        }
                        TopAppBar(
                            title = {
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            actions = {
                                Box {
                                    IconButton(
                                        onClick = { showAccountMenu = true },
                                        modifier = Modifier.testTag("account_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountCircle,
                                            contentDescription = "Account menu"
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showAccountMenu,
                                        onDismissRequest = { showAccountMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = currentUser.displayName ?: "Signed in",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = currentUser.email ?: "",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },
                                            onClick = {},
                                            enabled = false
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.sign_out)) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.AutoMirrored.Outlined.Logout,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                showAccountMenu = false
                                                signOut(credentialManager, scope)
                                            },
                                            modifier = Modifier.testTag("sign_out_menu_item")
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                },
                bottomBar = {
                    if (!useRail && isTopLevelRoute) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            TopLevelDestination.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                val label = stringResource(dest.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (!selected) {
                                            navController.navigate(dest.route) {
                                                popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (currentRoute == ROUTE_DASHBOARD || currentRoute == ROUTE_SCHOOLS) {
                        ExtendedFloatingActionButton(
                            onClick = openAddSchool,
                            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            text = { Text("Add School") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("fab_add_school")
                        )
                    }
                }
            ) { innerPadding ->
                when (val state = uiState) {
                    UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Sync Error",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = { signOut(credentialManager, scope) }) {
                                    Text("Sign out & retry")
                                }
                            }
                        }
                    }

                    is UiState.Success -> {
                        val snapshot = state.data
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_DASHBOARD,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable(ROUTE_DASHBOARD) {
                                DashboardScreen(
                                    snapshot = snapshot,
                                    onSelectProgressMode = viewModel::setProgressMode,
                                    onOpenUniversity = { uniId ->
                                        navController.navigate(universityDetailRoute(uniId))
                                    },
                                    onAddSchool = openAddSchool,
                                    onOpenCalendar = {
                                        navController.navigate(ROUTE_CALENDAR) {
                                            popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }

                            composable(ROUTE_SCHOOLS) {
                                SchoolsScreen(
                                    snapshot = snapshot,
                                    onOpenUniversity = { uniId ->
                                        navController.navigate(universityDetailRoute(uniId))
                                    },
                                    onAddSchool = openAddSchool
                                )
                            }

                            composable(ROUTE_CALENDAR) {
                                CalendarScreen(
                                    snapshot = snapshot,
                                    onOpenUniversity = { uniId ->
                                        navController.navigate(universityDetailRoute(uniId))
                                    }
                                )
                            }

                            composable(ROUTE_ESSAY_TYPES) {
                                EssayTypesScreen(
                                    snapshot = snapshot,
                                    isMutating = isMutating,
                                    onAddType = { name ->
                                        viewModel.addSupplementType(name, snapshot.supplementTypes)
                                    },
                                    onRenameType = { id, newName ->
                                        viewModel.renameSupplementType(id, newName)
                                    },
                                    onDeleteType = { id, name ->
                                        viewModel.deleteSupplementType(id, name)
                                    }
                                )
                            }

                            composable(
                                route = ROUTE_UNIVERSITY_DETAIL,
                                arguments = listOf(navArgument("universityId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val universityId = backStackEntry.arguments?.getString("universityId").orEmpty()
                                val university = snapshot.universities.firstOrNull { it.id == universityId }
                                if (university == null) {
                                    LaunchedEffect(universityId) {
                                        navController.popBackStack()
                                    }
                                } else {
                                    val uniSupplements = snapshot.supplementsByUniversity[university.id].orEmpty()
                                    val metrics = snapshot.progressByUniversity[university.id]
                                        ?: ProgressMetrics(0, 0, 0, 0, 0, 0, 0f, 0f)
                                    UniversityDetailScreen(
                                        university = university,
                                        supplements = uniSupplements,
                                        metrics = metrics,
                                        progressMode = snapshot.progressMode,
                                        onSelectProgressMode = viewModel::setProgressMode,
                                        onBack = { navController.popBackStack() },
                                        onEditSchool = { openEditSchool(university) },
                                        onDeleteSchool = {
                                            viewModel.deleteUniversity(university.id, university.name) {
                                                navController.popBackStack()
                                            }
                                        },
                                        onUpdateApplicationStatus = { newStatus ->
                                            viewModel.updateUniversityQuickStatus(
                                                universityId = university.id,
                                                applicationStatus = newStatus
                                            )
                                        },
                                        onUpdateFinAidStatus = { newDocStatus ->
                                            viewModel.updateUniversityQuickStatus(
                                                universityId = university.id,
                                                financialAidDocStatus = newDocStatus
                                            )
                                        },
                                        onGenerateMissingSlots = {
                                            viewModel.generateMissingSupplementSlots(
                                                universityId = university.id,
                                                currentCount = uniSupplements.size,
                                                targetCount = university.targetCountInt
                                            )
                                        },
                                        onAddSupplement = {
                                            supplementSheetState = university to null
                                        },
                                        onEditSupplement = { supp ->
                                            supplementSheetState = university to supp
                                        },
                                        onQuickCycleSupplementStatus = { supp ->
                                            val next = when (supp.statusEnum) {
                                                SupplementStatus.NOT_STARTED -> SupplementStatus.IN_PROGRESS
                                                SupplementStatus.IN_PROGRESS -> SupplementStatus.READY_TO_SUBMIT
                                                SupplementStatus.READY_TO_SUBMIT -> SupplementStatus.NOT_STARTED
                                            }
                                            viewModel.quickUpdateSupplementStatus(supp.id, next)
                                        }
                                    )
                                }
                            }
                        }

                        if (showSchoolEditor) {
                            val existing = editingUniversity
                            val existingCount = existing?.let {
                                snapshot.supplementsByUniversity[it.id]?.size ?: 0
                            } ?: 0
                            SchoolEditorSheet(
                                existingUniversity = existing,
                                existingSupplementCount = existingCount,
                                isSaving = isMutating,
                                onDismiss = { showSchoolEditor = false },
                                onSave = { input ->
                                    viewModel.saveUniversity(input, existingCount) {
                                        showSchoolEditor = false
                                    }
                                }
                            )
                        }

                        val suppEdit = supplementSheetState
                        if (suppEdit != null) {
                            val (uni, existingSupp) = suppEdit
                            SupplementEditorSheet(
                                universityId = uni.id,
                                universityName = uni.name,
                                existingSupplement = existingSupp,
                                availableTypes = snapshot.supplementTypes,
                                isSaving = isMutating,
                                onDismiss = { supplementSheetState = null },
                                onSave = { input ->
                                    viewModel.saveSupplement(input, snapshot.supplementTypes) {
                                        supplementSheetState = null
                                    }
                                },
                                onDelete = if (existingSupp != null) {
                                    { suppId ->
                                        viewModel.deleteSupplement(suppId) {
                                            supplementSheetState = null
                                        }
                                    }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
