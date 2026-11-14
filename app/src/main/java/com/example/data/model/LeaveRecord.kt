package com.example.data.model

data class LeaveRecord(
    val idIzin: String,
    val idSantri: String,
    val namaSantri: String,
    val kelas: String,
    val unit: String,
    val tanggalIzin: String,
    val tanggalKembali: String,
    val alasan: String,
    val status: String,
    val timestamp: String
)
