package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.DentalDatabase
import com.example.data.model.AppointmentEntity
import com.example.data.model.InventoryEntity
import com.example.data.model.PatientEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.DentalRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = DentalDatabase.getDatabase(this)
        val repository = DentalRepository(database.dentalDao())
        val factory = DentalViewModelFactory(repository)

        setContent {
            val viewModel: DentalViewModel = viewModel(factory = factory)
            val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()

            DentalBiTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainHubScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainHubScreen(viewModel: DentalViewModel) {
    val currentTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val role by viewModel.currentRole.collectAsStateWithLifecycle()
    val kpis by viewModel.kpiMetrics.collectAsStateWithLifecycle()
    val exportMsg by viewModel.exportMessage.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()

    val config = LocalConfiguration.current
    val isWideScreen = config.screenWidthDp >= 720

    // Temporary Dialog forms states
    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddAptDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportMsg) {
        exportMsg?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearExportMessage()
        }
    }

    Scaffold(
        snackbarHostStatus = snackbarHostState,
        topBar = {
            TopAppBarSaaS(
                isDark = isDark,
                currentRole = role,
                onRoleChange = { viewModel.setRole(it) },
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onExportRequest = { viewModel.exportReport(it) },
                isExporting = isExporting
            )
        },
        bottomBar = {
            if (!isWideScreen) {
                BottomNavBarSaaS(
                    activeTab = currentTab,
                    onTabSelect = { viewModel.setActiveTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Lateral Nav Rail for tablet or wide views
            if (isWideScreen) {
                SidebarNavSaaS(
                    activeTab = currentTab,
                    onTabSelect = { viewModel.setActiveTab(it) }
                )
                VerticalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    when (currentTab) {
                        ActiveTab.DASHBOARD -> DashboardTab(
                            kpis = kpis,
                            viewModel = viewModel,
                            onAddTransaction = { showAddTxDialog = true },
                            onAddAppointment = { showAddAptDialog = true }
                        )
                        ActiveTab.FINANCEIRO -> FinanceiroTab(
                            viewModel = viewModel,
                            onAddTransaction = { showAddTxDialog = true }
                        )
                        ActiveTab.AGENDA -> AgendaTab(
                            viewModel = viewModel,
                            onAddAppointment = { showAddAptDialog = true }
                        )
                        ActiveTab.IA_PREDITIVA -> IaPreditivaTab(
                            viewModel = viewModel
                        )
                        ActiveTab.CRM_NPS -> CrmNpsTab(
                            viewModel = viewModel
                        )
                        ActiveTab.ESTOQUE -> EstoqueTab(
                            viewModel = viewModel
                        )
                        ActiveTab.INTEGRACOES -> IntegracoesTab(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // Modal dialogs for operational insertion
    if (showAddTxDialog) {
        AddTransactionModal(
            onDismiss = { showAddTxDialog = false },
            onSave = { name, spec, value, cost ->
                viewModel.insertTransaction(name, spec, value, cost)
                showAddTxDialog = false
            }
        )
    }

    if (showAddAptDialog) {
        AddAppointmentModal(
            onDismiss = { showAddAptDialog = false },
            onSave = { name, spec, chair, time ->
                viewModel.insertAppointment(name, spec, chair, time)
                showAddAptDialog = false
            }
        )
    }
}

// -----------------------------------------------------------------
// A. Top Action bar of high-end SaaS dashboard
// -----------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarSaaS(
    isDark: Boolean,
    currentRole: UserRole,
    onRoleChange: (UserRole) -> Unit,
    onToggleDarkMode: () -> Unit,
    onExportRequest: (String) -> Unit,
    isExporting: Boolean
) {
    var showRoleMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DentalBluePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Dental BI",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Box(
                    modifier = Modifier
                        .background(DentalSuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "SaaS 2026",
                        color = DentalSuccessGreen,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        },
        actions = {
            // Dark Mode switch
            IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier.testTag("dark_mode_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Dark Mode",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Role selection / Permissões switcher
            Box {
                Button(
                    onClick = { showRoleMenu = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("permissao_role_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentRole.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                DropdownMenu(
                    expanded = showRoleMenu,
                    onDismissRequest = { showRoleMenu = false }
                ) {
                    UserRole.values().forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.name) },
                            onClick = {
                                onRoleChange(role)
                                showRoleMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Simulated exporter trigger
            Box {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(end = 4.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    IconButton(
                        onClick = { showExportMenu = true },
                        modifier = Modifier.testTag("export_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export"
                        )
                    }
                }
                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Exportar em PDF") },
                        onClick = {
                            onExportRequest("PDF")
                            showExportMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Exportar em Excel") },
                        onClick = {
                            onExportRequest("Excel")
                            showExportMenu = false
                        }
                    )
                }
            }
        }
    )
}

// Custom scaffold alert helper
@Composable
fun Scaffold(
    snackbarHostStatus: SnackbarHostState,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostStatus) },
        content = content
    )
}

// -----------------------------------------------------------------
// B. Compact Bottom Navigation Bar
// -----------------------------------------------------------------
@Composable
fun BottomNavBarSaaS(
    activeTab: ActiveTab,
    onTabSelect: (ActiveTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.navigationBarsPadding()
    ) {
        val navItems = listOf(
            Triple(ActiveTab.DASHBOARD, "Painel", Icons.Default.Dashboard),
            Triple(ActiveTab.FINANCEIRO, "Finanças", Icons.Default.MonetizationOn),
            Triple(ActiveTab.AGENDA, "Agenda", Icons.Default.DateRange),
            Triple(ActiveTab.IA_PREDITIVA, "IA", Icons.Default.Star),
            Triple(ActiveTab.CRM_NPS, "CRM", Icons.Default.People)
        )

        navItems.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = activeTab == tab,
                onClick = { onTabSelect(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

// -----------------------------------------------------------------
// C. Wide Navigation Sidebar
// -----------------------------------------------------------------
@Composable
fun SidebarNavSaaS(
    activeTab: ActiveTab,
    onTabSelect: (ActiveTab) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        val navItems = listOf(
            Triple(ActiveTab.DASHBOARD, "Painel", Icons.Default.Dashboard),
            Triple(ActiveTab.FINANCEIRO, "Financeiro", Icons.Default.MonetizationOn),
            Triple(ActiveTab.AGENDA, "Agenda", Icons.Default.DateRange),
            Triple(ActiveTab.IA_PREDITIVA, "IA Preditiva", Icons.Default.Star),
            Triple(ActiveTab.CRM_NPS, "CRM e NPS", Icons.Default.People),
            Triple(ActiveTab.ESTOQUE, "Estoque", Icons.Default.ShoppingCart),
            Triple(ActiveTab.INTEGRACOES, "Integrações", Icons.Default.Settings)
        )

        navItems.forEach { (tab, label, icon) ->
            NavigationRailItem(
                selected = activeTab == tab,
                onClick = { onTabSelect(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, overflow = TextOverflow.Ellipsis, maxLines = 1) },
                modifier = Modifier.padding(vertical = 4.dp).testTag("sidebar_${tab.name.lowercase()}")
            )
        }
    }
}

// -----------------------------------------------------------------
// TAB 1: DASHBOARD PRINCIPAL
// -----------------------------------------------------------------
@Composable
fun DashboardTab(
    kpis: KpiMetrics,
    viewModel: DentalViewModel,
    onAddTransaction: () -> Unit,
    onAddAppointment: () -> Unit
) {
    val txs by viewModel.transactions.collectAsStateWithLifecycle()
    val apts by viewModel.appointments.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming & Overview Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard Estratégico",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Análise integral de performance em tempo real • 2026",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    )
                }

                if (currentRole == UserRole.ADMINISTRADOR || currentRole == UserRole.RECEPCAO) {
                    FilledTonalButton(
                        onClick = onAddAppointment,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DentalBluePrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("dashboard_add_apt_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Novo Agendamento")
                    }
                }
            }
        }

        // BI Cards Row (Grid simulated with Rows for perfect responsivenes)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Faturamento Mensal",
                        value = "R$ ${String.format("%,.0f", kpis.monthlyRevenue)}",
                        subtext = "Faturamento global",
                        icon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = DentalBluePrimary) },
                        growth = kpis.monthlyRevenueGrowth,
                        testTag = "kpi_faturamento",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Lucro Líquido",
                        value = "R$ ${String.format("%,.0f", kpis.netProfit)}",
                        subtext = "Margem total livre",
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DentalSuccessGreen) },
                        growth = kpis.netProfitGrowth,
                        testTag = "kpi_lucro",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Taxa Ocupação",
                        value = "${String.format("%.1f", kpis.occupancyRate)}%",
                        subtext = "Agendas produtivas",
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = DentalWarningOrange) },
                        growth = 4.1,
                        testTag = "kpi_ocupacao",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Taxa No-show",
                        value = "${String.format("%.1f", kpis.noShowRate)}%",
                        subtext = "Faltas e cancelamentos",
                        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = DentalErrorRed) },
                        growth = -12.5, // Reduced cancellations is positive
                        testTag = "kpi_noshow",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Procedural & Operational charts
        if (currentRole == UserRole.ADMINISTRADOR || currentRole == UserRole.FINANCEIRO) {
            item {
                ProfitabilityBarChart(transactions = txs)
            }
        }

        item {
            ClinicaChairMap(
                appointments = apts,
                onEncaixeSuggested = { name, chair ->
                    viewModel.insertAppointment(name, "Limpeza", chair, "12:30")
                }
            )
        }
    }
}

// -----------------------------------------------------------------
// TAB 2: GESTÃO FINANCEIRA
// -----------------------------------------------------------------
@Composable
fun FinanceiroTab(
    viewModel: DentalViewModel,
    onAddTransaction: () -> Unit
) {
    val txs by viewModel.transactions.collectAsStateWithLifecycle()
    val kpis by viewModel.kpiMetrics.collectAsStateWithLifecycle()
    val role by viewModel.currentRole.collectAsStateWithLifecycle()

    var showExpenseWarning = txs.any { it.operationCost > 2000.0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gestão Financeira & Rentabilidade",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Lucratividade por procedimentos clínicos e análise de custo fixo",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    )
                }

                if (role == UserRole.ADMINISTRADOR || role == UserRole.FINANCEIRO) {
                    Button(
                        onClick = onAddTransaction,
                        colors = ButtonDefaults.buttonColors(containerColor = DentalBluePrimary),
                        modifier = Modifier.testTag("nova_receita_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Registrar Serviço")
                    }
                }
            }
        }

        // Expense Warning Alert
        if (showExpenseWarning && (role == UserRole.ADMINISTRADOR || role == UserRole.FINANCEIRO)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DentalErrorRed.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, DentalErrorRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DentalErrorRed)
                        Column {
                            Text(
                                "Alerta BI: Despesas Elevadas Diagnosticadas!",
                                fontWeight = FontWeight.Bold,
                                color = DentalErrorRed
                            )
                            Text(
                                "O custo operacional do procedimento 'Facetas Resina Composta' extrapolou R$ 3.000,00 neste mês. Recomendamos auditoria de insumos fornecedores.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Core dynamic financial summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Fluxo de Caixa Inteligente", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Receitas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("R$ ${String.format("%,.2f", kpis.monthlyRevenue)}", fontWeight = FontWeight.Bold, color = DentalBluePrimary, style = MaterialTheme.typography.titleLarge)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Despesas Operacionais", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            val despesa = kpis.monthlyRevenue - kpis.netProfit
                            Text("R$ ${String.format("%,.2f", despesa)}", fontWeight = FontWeight.Bold, color = DentalErrorRed, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }

        // Live list of transactions
        item {
            Text("Lançamentos de Serviços (Recentes)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        items(txs) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("transaction_card_${item.id}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(DentalBluePrimary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DentalBluePrimary)
                        }
                        Column {
                            Text(item.procedName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Especialidade: ${item.speciality} • Custo: R$ ${String.format("%.0f", item.operationCost)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "+ R$ ${String.format("%,.0f", item.amount)}",
                            fontWeight = FontWeight.Bold,
                            color = DentalSuccessGreen,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Lucro: R$ ${String.format("%.0f", item.netProfit)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DentalBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 3: GESTÃO DE AGENDA
// -----------------------------------------------------------------
@Composable
fun AgendaTab(
    viewModel: DentalViewModel,
    onAddAppointment: () -> Unit
) {
    val apts by viewModel.appointments.collectAsStateWithLifecycle()
    val role by viewModel.currentRole.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gestão de Agenda & Cadeiras",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Monitoramento das agendas odontológicas e tratamentos de ociosidade em 2026",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    )
                }

                if (role == UserRole.ADMINISTRADOR || role == UserRole.RECEPCAO) {
                    Button(
                        onClick = onAddAppointment,
                        colors = ButtonDefaults.buttonColors(containerColor = DentalBluePrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Inserir Agenda")
                    }
                }
            }
        }

        // Realtime Seat maps
        item {
            ClinicaChairMap(
                appointments = apts,
                onEncaixeSuggested = { name, chair ->
                    viewModel.insertAppointment(name, "Consulta Geral", chair, "14:15")
                }
            )
        }

        // Live schedule List
        item {
            Text("Horários e Consultas do Dia", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        items(apts) { item ->
            val flagBg = when (item.status) {
                "Atendido" -> DentalSuccessGreen.copy(alpha = 0.15f)
                "Confirmado" -> DentalBluePrimary.copy(alpha = 0.15f)
                "No-Show" -> DentalErrorRed.copy(alpha = 0.15f)
                else -> Color.LightGray.copy(alpha = 0.3f)
            }
            val flagColor = when (item.status) {
                "Atendido" -> DentalSuccessGreen
                "Confirmado" -> DentalBluePrimary
                "No-Show" -> DentalErrorRed
                else -> Color.DarkGray
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("appointment_card_${item.id}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Clock box
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                item.appointmentTime,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text(item.patientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Especialidade: ${item.speciality} • Cadeira: ${item.chairNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Status Dropdown actions for Secretariat
                    if (role == UserRole.ADMINISTRADOR || role == UserRole.RECEPCAO) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(flagBg, RoundedCornerShape(6.dp))
                                    .padding(vertical = 4.dp, horizontal = 10.dp)
                            ) {
                                Text(
                                    item.status.uppercase(),
                                    color = flagColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            // Dynamic check-in buttons
                            if (item.status == "Confirmado") {
                                IconButton(
                                    onClick = { viewModel.updateAppointmentStatus(item, "Atendido") },
                                    modifier = Modifier.size(32.dp).testTag("finish_btn_${item.id}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Concluir Atendimento", tint = DentalSuccessGreen)
                                }
                                IconButton(
                                    onClick = { viewModel.updateAppointmentStatus(item, "No-Show") },
                                    modifier = Modifier.size(32.dp).testTag("noshow_btn_${item.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Marcar No-show", tint = DentalErrorRed)
                                }
                            }
                        }
                    } else {
                        // Simple static read status
                        Box(
                            modifier = Modifier
                                .background(flagBg, RoundedCornerShape(6.dp))
                                .padding(vertical = 4.dp, horizontal = 10.dp)
                        ) {
                            Text(
                                item.status.uppercase(),
                                color = flagColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 4: INTELIGÊNCIA ARTIFICIAL PREDITIVA
// -----------------------------------------------------------------
@Composable
fun IaPreditivaTab(
    viewModel: DentalViewModel
) {
    val insights by viewModel.aiInsights.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingAi.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Inteligência Artificial Preditiva",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Previsões de demandas clínicas, no-show probabilístico e campanhas baseadas em IA para o ano de 2026",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = DentalBluePrimary, modifier = Modifier.size(28.dp))
                        Text(
                            "Consultor de BI Preditivo (Gemini 3.5 Flash integrado)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "O algoritmo gera análises automáticas cruzando faturamento, sazonalidade histórica, nível crítico de insumos odontológicos, taxa de NPS e previsões comportamentais de faltas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.fetchAISuggestions() },
                        colors = ButtonDefaults.buttonColors(containerColor = DentalBluePrimary),
                        modifier = Modifier.fillMaxWidth().testTag("gerar_insights_ia_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Processar IA Preditiva Agora")
                        }
                    }
                }
            }
        }

        // Result insight box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Relatório Estratégico de IA (Saída)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (insights.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Text(
                                "Relatório de IA pendente de processamento.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                "Clique no botão acima para consolidar insights preditivos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = insights,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 5: CRM E PESQUISA DE SATISFAÇÃO (NPS)
// -----------------------------------------------------------------
@Composable
fun CrmNpsTab(
    viewModel: DentalViewModel
) {
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val surveys by viewModel.surveys.collectAsStateWithLifecycle()
    val kpis by viewModel.kpiMetrics.collectAsStateWithLifecycle()

    var patientNameInput by remember { mutableStateOf("") }
    var npsScoreInput by remember { mutableStateOf(10) }
    var feedbackInput by remember { mutableStateOf("") }

    val hasDetractors = surveys.any { it.score < 6 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "CRM Odontológico & Experiência",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Gestão de tratamentos, conversão de propostas orçamentárias e fluxo NPS",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                )
            }
        }

        // Analytical summary curves
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NpsGauge(score = kpis.avgNps, modifier = Modifier.weight(1f))
                CrmBudgetFunnel(proposed = 31800.0, closed = 21700.0, modifier = Modifier.weight(1.2f))
            }
        }

        // Critical Detractor Alert
        if (hasDetractors) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DentalErrorRed.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, DentalErrorRed)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DentalErrorRed)
                        Column {
                            Text(
                                "ALERTA: Paciente Detrator Cadastrado!",
                                fontWeight = FontWeight.Bold,
                                color = DentalErrorRed
                            )
                            Text(
                                "Roberto Firmino enviou avaliação negativa (Nota 4) reclamando de reagendamentos de secretaria. O sistema agendou reconexão preventiva via WhatsApp.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Simulator manual NPS submit
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Automação de Envio: Registrar NPS Pós-Consulta", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = patientNameInput,
                        onValueChange = { patientNameInput = it },
                        label = { Text("Nome do Paciente") },
                        modifier = Modifier.fillMaxWidth().testTag("nps_paciente_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Nota do NPS (0 a 10):", fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = npsScoreInput.toFloat(),
                            onValueChange = { npsScoreInput = it.toInt() },
                            valueRange = 0f..10f,
                            steps = 9,
                            modifier = Modifier.weight(1f).testTag("nps_notarsl_slider")
                        )
                        Box(
                            modifier = Modifier.background(DentalBluePrimary, RoundedCornerShape(4.dp)).padding(6.dp)
                        ) {
                            Text("$npsScoreInput", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = feedbackInput,
                        onValueChange = { feedbackInput = it },
                        label = { Text("Comentários do Paciente") },
                        modifier = Modifier.fillMaxWidth().testTag("nps_comentarios_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (patientNameInput.isNotEmpty()) {
                                viewModel.submitNpsFeedback(patientNameInput, npsScoreInput, feedbackInput)
                                patientNameInput = ""
                                feedbackInput = ""
                                npsScoreInput = 10
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DentalSuccessGreen),
                        modifier = Modifier.fillMaxWidth().testTag("salvar_nps_btn")
                    ) {
                        Text("Simular Envio NPS Automatico")
                    }
                }
            }
        }

        // Pacientes list segmentation CRM
        item {
            Text("Funil CRM - Segmentação de Pacientes Clínicos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        items(patients) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("patient_card_${item.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Segmento: ${item.behaviorSegment} • Tratamento: ${item.statusTratamento}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Conversion status badges
                        Box(
                            modifier = Modifier
                                .background(DentalBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Orc. Fechado: R$ ${String.format("%.0f", item.budgetClosed)}",
                                color = DentalBluePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 6: ESTOQUE INTELIGENTE
// -----------------------------------------------------------------
@Composable
fun EstoqueTab(
    viewModel: DentalViewModel
) {
    val stock by viewModel.inventory.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Estoque Clínico Inteligente",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Avisos automáticos de ressuprimento baseados em procedimentos agendados em 2026",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                )
            }
        }

        // Critical item highlights
        val lowStockItems = stock.filter { it.quantity <= it.minQuantity }
        if (lowStockItems.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DentalWarningOrange.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, DentalWarningOrange)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DentalWarningOrange)
                        Column {
                            Text(
                                "Aviso: Nível Crítico de Estoque!",
                                fontWeight = FontWeight.Bold,
                                color = DentalWarningOrange
                            )
                            Text(
                                "Detectados ${lowStockItems.size} insumos odontológicos operando abaixo da margem mínima de segurança. Acione reposições.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        items(stock) { item ->
            val isLow = item.quantity <= item.minQuantity
            val statusColor = if (isLow) DentalErrorRed else DentalSuccessGreen
            val descStatus = if (isLow) "Reposição Necessária" else "Estoque Seguro"

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("inventory_item_${item.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Consumo Calculado: R$ ${String.format("%.2f", item.usageRatePerProcedure)} por procedimento",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Qty box
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${String.format("%.1f", item.quantity)} ${item.unit}",
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Min: ${String.format("%.0f", item.minQuantity)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                descStatus,
                                color = statusColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Add raw replenishment simulator
                        Button(
                            onClick = { viewModel.restockInventory(item, 10.0) },
                            colors = ButtonDefaults.buttonColors(containerColor = DentalBluePrimary),
                            modifier = Modifier.height(30.dp).testTag("abastecer_btn_${item.id}"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("+10 Unidades", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 7: INTEGRAÇÕES & SaaS SETTINGS
// -----------------------------------------------------------------
@Composable
fun IntegracoesTab(
    viewModel: DentalViewModel
) {
    val role by viewModel.currentRole.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Integrações via API e Segurança (Cloud)",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Conexões operacionais nativas com ERP, Agenda, Meta Ads e Prontuários",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configurações Multi-Tenant & SaaS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "O sistema centraliza acessos customizados baseados em perfis. Como seu perfil ativo atual é '${role.name}', suas permissões seguem as políticas reguladas abaixo:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Permissoes details
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, "Ativo", tint = DentalSuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Administrador: Acesso ilimitado e relatórios consolidados.")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, "Ativo", tint = DentalSuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dentista: Foco em Prontuário, NPS e Agenda clínica dedicada.")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, "Ativo", tint = DentalSuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recepção: Inserção ágil de pacientes e confirmação de cadeiras.")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, "Ativo", tint = DentalSuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Financeiro: Fluxos de caixa, auditamento de despesas e lucros.")
                        }
                    }
                }
            }
        }

        // Integration channel row
        item {
            Text("Interfaces de APIs Conectadas (Estáveis em 2026)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val integrationsList = listOf(
                    Triple("WhatsApp Business API", "Disparo automático de lembretes e confirmações 24h", true),
                    Triple("Google Calendar Sync", "Sincronização reativa bidirecional de consultório", true),
                    Triple("ERP Odontológico Prontuário", "Importação incremental de prontuários eletrônicos", true),
                    Triple("Sistemas de Notas Fiscais", "Geramento fiscal unificado automático", true),
                    Triple("Meta Ads (Facebook/Instagram)", "Importação de custos de campanhas estéticas", true)
                )

                integrationsList.forEach { (name, desc, isOnline) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }

                            Box(
                                modifier = Modifier
                                    .background(DentalSuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("CONECTADO", color = DentalSuccessGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// MODAL FORMS
// -----------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionModal(
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double) -> Unit
) {
    var textProced by remember { mutableStateOf("") }
    var textValue by remember { mutableStateOf("") }
    var textCost by remember { mutableStateOf("") }

    val specialities = listOf("Ortodontia", "Implantodontia", "Estética", "Limpeza", "Cirurgia")
    var expandedSpec by remember { mutableStateOf(false) }
    var selectedSpec by remember { mutableStateOf(specialities[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Novo Serviço Dental", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = textProced,
                    onValueChange = { textProced = it },
                    label = { Text("Nome do Procedimento") },
                    modifier = Modifier.fillMaxWidth().testTag("add_tx_proced_input")
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedSpec = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_tx_spec_dropdown")
                    ) {
                        Text("Especialidade: $selectedSpec")
                    }
                    DropdownMenu(
                        expanded = expandedSpec,
                        onDismissRequest = { expandedSpec = false }
                    ) {
                        specialities.forEach { spec ->
                            DropdownMenuItem(
                                text = { Text(spec) },
                                onClick = {
                                    selectedSpec = spec
                                    expandedSpec = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Receita / Preço Cobrado (R$)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_tx_value_input")
                )

                OutlinedTextField(
                    value = textCost,
                    onValueChange = { textCost = it },
                    label = { Text("Custo Operacional de Insumos (R$)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_tx_cost_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rValue = textValue.toDoubleOrNull() ?: 0.0
                    val rCost = textCost.toDoubleOrNull() ?: 0.0
                    if (textProced.isNotEmpty()) {
                        onSave(textProced, selectedSpec, rValue, rCost)
                    }
                },
                modifier = Modifier.testTag("salvar_tx_confirm_btn")
            ) {
                Text("Confirmar Lançamento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentModal(
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String) -> Unit
) {
    var textName by remember { mutableStateOf("") }
    var textTime by remember { mutableStateOf("09:00") }

    val specialities = listOf("Ortodontia", "Implantodontia", "Estética", "Limpeza", "Cirurgia")
    var expandedSpec by remember { mutableStateOf(false) }
    var selectedSpec by remember { mutableStateOf(specialities[0]) }

    val chairs = listOf(1, 2, 3)
    var expandedChair by remember { mutableStateOf(false) }
    var selectedChair by remember { mutableStateOf(chairs[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Agendamento Clínico", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = textName,
                    onValueChange = { textName = it },
                    label = { Text("Nome do Paciente") },
                    modifier = Modifier.fillMaxWidth().testTag("add_apt_name_input")
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedSpec = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_apt_spec_dropdown")
                    ) {
                        Text("Especialidade: $selectedSpec")
                    }
                    DropdownMenu(
                        expanded = expandedSpec,
                        onDismissRequest = { expandedSpec = false }
                    ) {
                        specialities.forEach { spec ->
                            DropdownMenuItem(
                                text = { Text(spec) },
                                onClick = {
                                    selectedSpec = spec
                                    expandedSpec = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedChair = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_apt_chair_dropdown")
                    ) {
                        Text("Cadeira / Consultório: $selectedChair")
                    }
                    DropdownMenu(
                        expanded = expandedChair,
                        onDismissRequest = { expandedChair = false }
                    ) {
                        chairs.forEach { chair ->
                            DropdownMenuItem(
                                text = { Text("Cadeira $chair") },
                                onClick = {
                                    selectedChair = chair
                                    expandedChair = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = textTime,
                    onValueChange = { textTime = it },
                    label = { Text("Horário (ex: 09:30)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_apt_time_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textName.isNotEmpty()) {
                        onSave(textName, selectedSpec, selectedChair, textTime)
                    }
                },
                modifier = Modifier.testTag("salvar_apt_confirm_btn")
            ) {
                Text("Agendar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
