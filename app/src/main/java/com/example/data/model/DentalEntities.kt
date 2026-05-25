package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val procedName: String,
    val speciality: String, // Ortodontia, Implantodontia, Estética, Limpeza, Cirurgia
    val amount: Double,
    val operationCost: Double,
    val netProfit: Double = amount - operationCost,
    val dateString: String, // YYYY-MM-DD for easy querying
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientName: String,
    val speciality: String,
    val chairNumber: Int, // 1 or 2 or 3
    val appointmentTime: String, // e.g. "09:30"
    val appointmentDate: String, // YYYY-MM-DD
    val status: String, // "Confirmado", "Cancelado", "No-Show", "Atendido"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Double,
    val minQuantity: Double,
    val unit: String,
    val usageRatePerProcedure: Double, // Estimated usage per related dental check
    val predictedWasteRate: Double // Percentage, e.g. 0.05 for 5% waste
)

@Entity(tableName = "nps_surveys")
data class NpsSurveyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientName: String,
    val score: Int, // 0 to 10
    val feedback: String,
    val dateString: String,
    val ratingTimeStr: String,
    val status: String // "Promotor", "Neutro", "Detrator"
)

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val age: Int,
    val phone: String,
    val email: String,
    val statusTratamento: String, // "Planejamento", "Em Progresso", "Finalizado"
    val conversionRate: Double, // Conversion of billing plan proposta, e.g. 0.85
    val budgetProposed: Double,
    val budgetClosed: Double,
    val behaviorSegment: String // "Fiel", "Faltoso Recorrente", "Preço-Sensível"
)
