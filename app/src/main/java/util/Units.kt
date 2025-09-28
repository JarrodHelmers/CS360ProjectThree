package com.example.weight_trackerapp.util

enum class UnitSystem { KG, LB }

// Conversion factor we’ll use everywhere.
private const val KG_TO_LB = 2.20462262185

// Convert from kilograms to pounds
fun kgToLb(kg: Double): Double = kg * KG_TO_LB

// Convert from pounds back to kilograms
fun lbToKg(lb: Double): Double = lb / KG_TO_LB