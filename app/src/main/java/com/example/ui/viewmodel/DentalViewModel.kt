package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DentalDatabase
import com.example.data.model.*
import com.example.data.remote.GeminiService
import com.example.data.repository.DentalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class UserRole {
    ADMINISTRADOR,
    DENTISTA,
    RECEPCAO,
    FINANCEIRO
}

enum class ActiveTab {
    DASHBOARD,
    FINANCEIRO,
    AGENDA,
    IA_PREDITIVA,
    CRM_NPS,
    ESTOQUE,
    INTEGRACOES
}

class DentalViewModel(private val repository: DentalRepository) : ViewModel() {

    // Global App Preferences
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.ADMINISTRADOR)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _activeTab = MutableStateFlow(ActiveTab.DASHBOARD)
    val activeTab: StateFlow<ActiveTab> = _activeTab.asStateFlow()

    // Gemini IA State
    private val _aiInsights = MutableStateFlow<String>("")
    val aiInsights: StateFlow<String> = _aiInsights.asStateFlow()

    private val _isLoadingAi = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    // Export State simulation
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    // Database Flows
    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appointments: StateFlow<List<AppointmentEntity>> = repository.allAppointments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventory: StateFlow<List<InventoryEntity>> = repository.allInventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val surveys: StateFlow<List<NpsSurveyEntity>> = repository.allSurveys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val patients: StateFlow<List<PatientEntity>> = repository.allPatients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically seed clinical data if database flows indicate empty lists
        checkAndSeedInitialData()
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setRole(role: UserRole) {
        _currentRole.value = role
    }

    fun setActiveTab(tab: ActiveTab) {
        _activeTab.value = tab
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }

    // Dynamic calculated KPI aggregates
    val kpiMetrics: StateFlow<KpiMetrics> = combine(
        transactions,
        appointments,
        surveys,
        patients
    ) { txs, apts, srvs, pts ->
        val totalRevenue = txs.sumOf { it.amount }
        val totalCost = txs.sumOf { it.operationCost }
        val netProfit = totalRevenue - totalCost

        val totalApts = apts.size
        val noShowApts = apts.count { it.status == "No-Show" }
        val canceledApts = apts.count { it.status == "Cancelado" }
        val noShowRate = if (totalApts > 0) (noShowApts.toDouble() / totalApts) * 100.0 else 0.0

        // Occupancy calculation (e.g., 24 potential slots a day. Chair count: 3)
        // Let's assume max capacity is 3 chairs * 8 slots = 24 slots/day. Current appointments on active day = size
        // We simulate a realistic occupancy of (active.size / max_slots)
        val maxPotentialSlots = 12.0
        val activeSlotsCount = apts.count { it.status == "Confirmado" || it.status == "Atendido" }
        val occupancyRate = minOf(95.0, maxOf(40.0, (activeSlotsCount.toDouble() / maxPotentialSlots) * 100.0))

        // Average satisfaction
        val avgNps = if (srvs.isNotEmpty()) srvs.map { it.score }.average() else 9.2

        // Marketing ROI simulation
        val marketingCost = 1500.0 // Fixed simulated budget
        val aestheticTxCount = txs.count { it.speciality == "Estética" }
        val aestheticIncome = txs.filter { it.speciality == "Estética" }.sumOf { it.amount }
        val simulatedRoi = if (marketingCost > 0) ((aestheticIncome - marketingCost) / marketingCost) * 100.0 else 250.0

        KpiMetrics(
            monthlyRevenue = totalRevenue,
            monthlyRevenueGrowth = 15.4, // Positive simulated contrast
            netProfit = netProfit,
            netProfitGrowth = 18.2,
            patientsAttended = apts.count { it.status == "Atendido" },
            occupancyRate = occupancyRate,
            noShowRate = noShowRate,
            avgNps = avgNps,
            marketingRoi = simulatedRoi,
            avgConsultTimeMinutes = 45
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        KpiMetrics()
    )

    // Data seeding check
    private fun checkAndSeedInitialData() {
        viewModelScope.launch {
            // Seeding if database is currently clean of data
            repository.allTransactions.collect { txs ->
                if (txs.isEmpty()) {
                    seedDatabase()
                }
            }
        }
    }

    private suspend fun seedDatabase() {
        val trans = listOf(
            TransactionEntity(procedName = "Facetas Resina Composta", speciality = "Estética", amount = 8500.0, operationCost = 3100.0, dateString = "2026-05-20"),
            TransactionEntity(procedName = "Aparelho Autoligável Inicial", speciality = "Ortodontia", amount = 3200.0, operationCost = 850.0, dateString = "2026-05-22"),
            TransactionEntity(procedName = "Implante Titânio", speciality = "Implantodontia", amount = 4500.0, operationCost = 1500.0, dateString = "2026-05-18"),
            TransactionEntity(procedName = "Profilaxia e Raspagem", speciality = "Limpeza", amount = 250.0, operationCost = 60.0, dateString = "2026-05-25"),
            TransactionEntity(procedName = "Extração Dental Siso", speciality = "Cirurgia", amount = 900.0, operationCost = 220.0, dateString = "2026-05-24"),
            TransactionEntity(procedName = "Clareamento Convencional Laser", speciality = "Estética", amount = 1200.0, operationCost = 350.0, dateString = "2026-05-15"),
            TransactionEntity(procedName = "Manutenção Ortodôntica Mensal", speciality = "Ortodontia", amount = 180.0, operationCost = 40.0, dateString = "2026-05-25"),
            TransactionEntity(procedName = "Profilaxia e Raspagem Geral", speciality = "Limpeza", amount = 250.0, operationCost = 60.0, dateString = "2026-05-23"),
            TransactionEntity(procedName = "Enxerto Ósseo BioOss", speciality = "Cirurgia", amount = 2800.0, operationCost = 950.0, dateString = "2026-05-12"),
            TransactionEntity(procedName = "Restauração Fotopolimerizável X", speciality = "Estética", amount = 350.0, operationCost = 90.0, dateString = "2026-05-21")
        )

        val apts = listOf(
            AppointmentEntity(patientName = "Arthur Pendragon", speciality = "Ortodontia", chairNumber = 1, appointmentTime = "08:00", appointmentDate = "2026-05-25", status = "Atendido"),
            AppointmentEntity(patientName = "Clara Albuquerque", speciality = "Estética", chairNumber = 2, appointmentTime = "09:15", appointmentDate = "2026-05-25", status = "Atendido"),
            AppointmentEntity(patientName = "Mariana Santos", speciality = "Implantodontia", chairNumber = 1, appointmentTime = "10:30", appointmentDate = "2026-05-25", status = "Confirmado"),
            AppointmentEntity(patientName = "Roberto Firmino", speciality = "Limpeza", chairNumber = 3, appointmentTime = "11:00", appointmentDate = "2026-05-25", status = "No-Show"),
            AppointmentEntity(patientName = "Beatriz Costa", speciality = "Cirurgia", chairNumber = 2, appointmentTime = "13:30", appointmentDate = "2026-05-25", status = "Confirmado"),
            AppointmentEntity(patientName = "Eduardo Souza", speciality = "Ortodontia", chairNumber = 1, appointmentTime = "15:00", appointmentDate = "2026-05-25", status = "Confirmado"),
            AppointmentEntity(patientName = "Júlia Lima", speciality = "Limpeza", chairNumber = 2, appointmentTime = "16:15", appointmentDate = "2026-05-25", status = "Cancelado"),
            AppointmentEntity(patientName = "Sandro Silva", speciality = "Estética", chairNumber = 3, appointmentTime = "17:00", appointmentDate = "2026-05-25", status = "Confirmado")
        )

        val stock = listOf(
            InventoryEntity(name = "Resina Composta Fotopolimerizável", quantity = 12.0, minQuantity = 25.0, unit = "tubos", usageRatePerProcedure = 0.35, predictedWasteRate = 0.08),
            InventoryEntity(name = "Anestésico Lidocaína 2%", quantity = 98.0, minQuantity = 50.0, unit = "tubetes", usageRatePerProcedure = 1.50, predictedWasteRate = 0.05),
            InventoryEntity(name = "Implante Dentário Conector Cônico", quantity = 4.0, minQuantity = 10.0, unit = "unidades", usageRatePerProcedure = 1.00, predictedWasteRate = 0.02),
            InventoryEntity(name = "Agulhas Gengivais 30G", quantity = 185.0, minQuantity = 80.0, unit = "unidades", usageRatePerProcedure = 1.20, predictedWasteRate = 0.04),
            InventoryEntity(name = "Luvas de Procedimento Látex (M)", quantity = 8.0, minQuantity = 12.0, unit = "caixas", usageRatePerProcedure = 2.00, predictedWasteRate = 0.12),
            InventoryEntity(name = "Gesso Dental Odontológico Pedra", quantity = 12.5, minQuantity = 5.0, unit = "kg", usageRatePerProcedure = 0.25, predictedWasteRate = 0.15)
        )

        val nps = listOf(
            NpsSurveyEntity(patientName = "Clara Albuquerque", score = 10, feedback = "Adorei minhas novas facetas, recuperou meu sorriso! Excelente atendimento das secretárias e carinho dos doutores.", dateString = "2026-05-24", ratingTimeStr = "11:20", status = "Promotor"),
            NpsSurveyEntity(patientName = "Arthur Pendragon", score = 9, feedback = "Aparelho incomoda um pouco na primeira semana, mas as consultas são rápidas e muito organizadas.", dateString = "2026-05-24", ratingTimeStr = "16:45", status = "Promotor"),
            NpsSurveyEntity(patientName = "Roberto Firmino", score = 4, feedback = "Remarcaram meu horário duas vezes por erro no sistema de agendamento de vocês. Precisa melhorar.", dateString = "2026-05-22", ratingTimeStr = "09:10", status = "Detrator"),
            NpsSurveyEntity(patientName = "Beatriz Costa", score = 8, feedback = "Procedimento cirúrgico limpo e sem dores. Muito pontuais.", dateString = "2026-05-20", ratingTimeStr = "15:30", status = "Promotor"),
            NpsSurveyEntity(patientName = "Sandro Silva", score = 9, feedback = "Atendimento clareamento perfeito, clínica moderna.", dateString = "2026-05-18", ratingTimeStr = "18:00", status = "Promotor")
        )

        val clients = listOf(
            PatientEntity(name = "Clara Albuquerque", age = 28, phone = "11988887777", email = "clara@email.com", statusTratamento = "Finalizado", conversionRate = 1.00, budgetProposed = 8500.0, budgetClosed = 8500.0, behaviorSegment = "Fiel"),
            PatientEntity(name = "Arthur Pendragon", age = 35, phone = "11977776666", email = "arthur@email.com", statusTratamento = "Em Progresso", conversionRate = 1.00, budgetProposed = 3200.0, budgetClosed = 3200.0, behaviorSegment = "Fiel"),
            PatientEntity(name = "Roberto Firmino", age = 42, phone = "21966665555", email = "roberto@email.com", statusTratamento = "Planejamento", conversionRate = 0.50, budgetProposed = 4500.0, budgetClosed = 0.0, behaviorSegment = "Faltoso Recorrente"),
            PatientEntity(name = "Beatriz Costa", age = 31, phone = "31955554444", email = "beatriz@email.com", statusTratamento = "Em Progresso", conversionRate = 1.00, budgetProposed = 900.0, budgetClosed = 900.0, behaviorSegment = "Fiel"),
            PatientEntity(name = "Gisele Nogueira", age = 55, phone = "11944443333", email = "gisele@email.com", statusTratamento = "Planejamento", conversionRate = 0.15, budgetProposed = 15000.0, budgetClosed = 0.0, behaviorSegment = "Preço-Sensível")
        )

        repository.seedDbIfEmpty(trans, apts, stock, nps, clients)
    }

    // Interactive custom addition triggers to update BI states on-the-fly
    fun insertTransaction(procedName: String, speciality: String, amount: Double, cost: Double) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    procedName = procedName,
                    speciality = speciality,
                    amount = amount,
                    operationCost = cost,
                    dateString = "2026-05-25"
                )
            )
        }
    }

    fun insertAppointment(patientName: String, speciality: String, chair: Int, time: String) {
        viewModelScope.launch {
            repository.insertAppointment(
                AppointmentEntity(
                    patientName = patientName,
                    speciality = speciality,
                    chairNumber = chair,
                    appointmentTime = time,
                    appointmentDate = "2026-05-25",
                    status = "Confirmado"
                )
            )
        }
    }

    fun updateAppointmentStatus(appointment: AppointmentEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateAppointment(
                appointment.copy(status = newStatus)
            )
        }
    }

    fun restockInventory(item: InventoryEntity, addedQty: Double) {
        viewModelScope.launch {
            repository.updateInventory(
                item.copy(quantity = item.quantity + addedQty)
            )
        }
    }

    fun submitNpsFeedback(patientName: String, score: Int, feedback: String) {
        viewModelScope.launch {
            val status = when {
                score >= 9 -> "Promotor"
                score >= 7 -> "Neutro"
                else -> "Detrator"
            }
            repository.insertSurvey(
                NpsSurveyEntity(
                    patientName = patientName,
                    score = score,
                    feedback = feedback,
                    dateString = "2026-05-25",
                    ratingTimeStr = "12:00",
                    status = status
                )
            )
        }
    }

    // Run AI strategic calculations using Gemini API or offline statistical backup
    fun fetchAISuggestions() {
        viewModelScope.launch {
            _isLoadingAi.value = true
            val metrics = kpiMetrics.value
            val invItems = inventory.value
            val criticallyLow = invItems.filter { it.quantity < it.minQuantity }
            val estoqueDesc = if (criticallyLow.isEmpty()) {
                "Insumos todos em níveis seguros."
            } else {
                criticallyLow.joinToString(", ") { "${it.name} (${String.format("%.1f", it.quantity)} / ${String.format("%.1f", it.minQuantity)} ${it.unit})" } + " estão abaixo do estoque de segurança."
            }

            val result = GeminiService.getStrategicInsights(
                faturamentoMensal = metrics.monthlyRevenue,
                lucroLiquido = metrics.netProfit,
                noShowRate = metrics.noShowRate,
                taxaOcupacao = metrics.occupancyRate,
                pacientesAtendidos = metrics.patientsAttended,
                npsMedio = metrics.avgNps,
                estoqueCriticoInfo = estoqueDesc
            )
            _aiInsights.value = result
            _isLoadingAi.value = false
        }
    }

    // Simulates report generation/downloading
    fun exportReport(format: String) {
        viewModelScope.launch {
            _isExporting.value = true
            kotlinx.coroutines.delay(1800) // Aesthetic visual loading
            _isExporting.value = false
            _exportMessage.value = "Relatório de BI Dental completo gerado com sucesso em formato $format! Salvo na pasta downloads."
        }
    }
}

// Data class transfer metrics holder
data class KpiMetrics(
    val monthlyRevenue: Double = 0.0,
    val monthlyRevenueGrowth: Double = 0.0,
    val netProfit: Double = 0.0,
    val netProfitGrowth: Double = 0.0,
    val patientsAttended: Int = 0,
    val occupancyRate: Double = 0.0,
    val noShowRate: Double = 0.0,
    val avgNps: Double = 0.0,
    val marketingRoi: Double = 0.0,
    val avgConsultTimeMinutes: Int = 0
)

// Factory Provider
class DentalViewModelFactory(private val repository: DentalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DentalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DentalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
