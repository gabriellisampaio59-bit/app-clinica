package com.example.data.repository

import com.example.data.local.DentalDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class DentalRepository(private val dentalDao: DentalDao) {

    // Flows
    val allTransactions: Flow<List<TransactionEntity>> = dentalDao.getAllTransactions()
    val allAppointments: Flow<List<AppointmentEntity>> = dentalDao.getAllAppointments()
    val allInventory: Flow<List<InventoryEntity>> = dentalDao.getAllInventory()
    val allSurveys: Flow<List<NpsSurveyEntity>> = dentalDao.getAllSurveys()
    val allPatients: Flow<List<PatientEntity>> = dentalDao.getAllPatients()

    // Suspend operations for Transactions
    suspend fun insertTransaction(transaction: TransactionEntity) {
        dentalDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        dentalDao.deleteTransaction(transaction)
    }

    // Suspend operations for Appointments
    suspend fun insertAppointment(appointment: AppointmentEntity) {
        dentalDao.insertAppointment(appointment)
    }

    suspend fun updateAppointment(appointment: AppointmentEntity) {
        dentalDao.updateAppointment(appointment)
    }

    suspend fun deleteAppointment(appointment: AppointmentEntity) {
        dentalDao.deleteAppointment(appointment)
    }

    // Suspend operations for Inventory
    suspend fun insertInventory(item: InventoryEntity) {
        dentalDao.insertInventory(item)
    }

    suspend fun updateInventory(item: InventoryEntity) {
        dentalDao.updateInventory(item)
    }

    suspend fun deleteInventory(item: InventoryEntity) {
        dentalDao.deleteInventory(item)
    }

    // Suspend operations for Surveys
    suspend fun insertSurvey(survey: NpsSurveyEntity) {
        dentalDao.insertSurvey(survey)
    }

    suspend fun deleteSurvey(survey: NpsSurveyEntity) {
        dentalDao.deleteSurvey(survey)
    }

    // Suspend operations for Patients
    suspend fun insertPatient(patient: PatientEntity) {
        dentalDao.insertPatient(patient)
    }

    suspend fun updatePatient(patient: PatientEntity) {
        dentalDao.updatePatient(patient)
    }

    suspend fun deletePatient(patient: PatientEntity) {
        dentalDao.deletePatient(patient)
    }

    // Bulk seed
    suspend fun seedDbIfEmpty(
        transactions: List<TransactionEntity>,
        appointments: List<AppointmentEntity>,
        inventory: List<InventoryEntity>,
        surveys: List<NpsSurveyEntity>,
        patients: List<PatientEntity>
    ) {
        dentalDao.insertTransactions(transactions)
        dentalDao.insertAppointments(appointments)
        dentalDao.insertInventoryList(inventory)
        dentalDao.insertSurveys(surveys)
        dentalDao.insertPatients(patients)
    }
}
