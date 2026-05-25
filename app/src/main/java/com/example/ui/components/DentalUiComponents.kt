package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.*

// -----------------------------------------------------------------
// 1. KPI Metric Card Component
// -----------------------------------------------------------------
@Composable
fun KpiCard(
    title: String,
    value: String,
    subtext: String,
    icon: @Composable () -> Unit,
    growth: Double? = null,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (growth != null) {
                    val isPos = growth >= 0
                    val badgeBg = if (isPos) DentalSuccessGreen.copy(alpha = 0.15f) else DentalErrorRed.copy(alpha = 0.15f)
                    val badgeColor = if (isPos) DentalSuccessGreen else DentalErrorRed
                    val sign = if (isPos) "+" else ""

                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$sign${String.format("%.1f", growth)}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        )
                    }
                }
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// 2. Custom Draw: Procedure Profitability Chart (Estética, Ortodontia, etc.)
// -----------------------------------------------------------------
@Composable
fun ProfitabilityBarChart(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    // Group and aggregate data
    val grouped = transactions.groupBy { it.speciality }
    val data = listOf("Ortodontia", "Implantodontia", "Estética", "Limpeza", "Cirurgia").map { spec ->
        val txsForSpec = grouped[spec] ?: emptyList()
        val totalRevenue = txsForSpec.sumOf { it.amount }
        val totalCost = txsForSpec.sumOf { it.operationCost }
        val finalProfit = totalRevenue - totalCost
        SpecProfitData(
            speciality = spec,
            revenue = totalRevenue,
            profit = finalProfit
        )
    }

    val maxVal = data.maxOfOrNull { maxOf(it.revenue, 1.0) } ?: 10000.0

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Rentabilidade por Especialidade (R$)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Contrastando as receitas brutas (azul) contra lucro líquido (verde)",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                data.forEach { item ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.speciality,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "Lucro: R$ ${String.format("%.0f", item.profit)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DentalSuccessGreen
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        val revenueRatio = (item.revenue / maxVal).toFloat()
                        val profitRatio = (item.profit / maxVal).toFloat()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        ) {
                            // Revenue Bar (Blue)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(revenueRatio)
                                    .background(DentalBluePrimary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            )
                            // Profit Bar (Green overlays)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(profitRatio)
                                    .background(DentalSuccessGreen, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SpecProfitData(val speciality: String, val revenue: Double, val profit: Double)

// -----------------------------------------------------------------
// 3. Interactive Seat Occupation Map Component
// -----------------------------------------------------------------
@Composable
fun ClinicaChairMap(
    appointments: List<AppointmentEntity>,
    onEncaixeSuggested: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic chair grouping (Cadeiras 1, 2, 3)
    val totalChairs = 3
    val positions = (1..totalChairs).map { chair ->
        appointments.firstOrNull { it.chairNumber == chair && (it.status == "Confirmado" || it.status == "Atendido") }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Mapa de Cadeiras em Tempo Real",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Monitoramento imediato de consultórios e detecção de ociosidades",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                positions.forEachIndexed { index, currentApt ->
                    val chairNo = index + 1
                    val isOcupied = currentApt != null
                    val chairBg = if (isOcupied) DentalBluePrimary.copy(alpha = 0.08f) else DentalWarningOrange.copy(alpha = 0.08f)
                    val borderColor = if (isOcupied) DentalBluePrimary else DentalWarningOrange

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .background(chairBg, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(borderColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isOcupied) Icons.Default.PersonalVideo else Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = borderColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cadeira $chairNo",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (isOcupied && currentApt != null) {
                            Text(
                                text = "Ocupada",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DentalBluePrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentApt.patientName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${currentApt.speciality} • ${currentApt.appointmentTime}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            )
                        } else {
                            Text(
                                text = "OCIOSA",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DentalWarningOrange
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onEncaixeSuggested("Paciente Ocioso - Cadeira $chairNo", chairNo) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("sugestao_encaixe_btn_$chairNo"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DentalWarningOrange,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Encaixar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// 4. NPS Radial Gauge Meter
// -----------------------------------------------------------------
@Composable
fun NpsGauge(
    score: Double,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "NpsValue"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Índice de Satisfação NPS",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Feedback imediato pós consulta do paciente",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    // Gray background circle arc
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Active score arc
                    val sweep = (animatedScore / 10f) * 270f
                    val colorBrush = Brush.horizontalGradient(
                        colors = listOf(DentalWarningOrange, DentalSuccessGreen)
                    )

                    drawArc(
                        brush = colorBrush,
                        startAngle = 135f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(
                    modifier = Modifier.offset(y = (-4).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format("%.1f", score),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Zona de Excelência",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DentalSuccessGreen
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// -----------------------------------------------------------------
// 5. CRM Conversion Funnel
// -----------------------------------------------------------------
@Composable
fun CrmBudgetFunnel(
    proposed: Double,
    closed: Double,
    modifier: Modifier = Modifier
) {
    val conversionRate = if (proposed > 0.0) (closed / proposed) * 100.0 else 72.5

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Funil de Fechamento de Orçamentos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Taxa média de conversão em vendas de tratamentos planejados",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stage 1: Proposals Created
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Orçamentos Propostos",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "R$ ${String.format("%,.0f", proposed)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(DentalBluePrimary, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Base do Funil (100%)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Stage 2: Proposals Converted
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Orçamentos Convertidos",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "R$ ${String.format("%,.0f", closed)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DentalSuccessGreen
                            )
                        )
                    }
                    val funnelScale = (conversionRate / 100f).toFloat().coerceIn(0.2f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(funnelScale)
                            .height(28.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(DentalSuccessGreen, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Conversão: ${String.format("%.1f", conversionRate)}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
