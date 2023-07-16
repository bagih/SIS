package com.bagih.sis.data

data class Cattle(
    val name: String,
    val score: Float,
    val NIS: String,
    val race: String,
    val age: Int,
    val location: String,
    val gender: cattleGender,
    val isHealthy: Boolean,
    val birth_date: String,
    val parentM: String,
    val parentF: String,
    val owner: String
)

enum class cattleGender{
    MALE,
    FEMALE
}

