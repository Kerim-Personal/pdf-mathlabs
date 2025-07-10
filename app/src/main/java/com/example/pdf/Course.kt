package com.example.pdf // Kendi paket adınla eşleştiğinden emin ol

data class Course(
    val title: String,       // Dersin başlığı (örneğin, "Kalkülüs I")
    val topics: List<String>, // Derse ait konu başlıklarının bir listesi
    var isExpanded: Boolean = false // Akordiyon bölümünün açık mı kapalı mı olduğunu takip eder
)