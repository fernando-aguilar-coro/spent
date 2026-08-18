package com.app.spent.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
data class CategoryIconOption(
val iconName: String,
val icon: ImageVector,
val label: String
)

object CategoryIconHelper {

  val availableIcons = listOf(
  CategoryIconOption("ShoppingCart", Icons.Default.ShoppingCart, "Groceries"),
  CategoryIconOption("Restaurant", Icons.Default.Restaurant, "Food"),
  CategoryIconOption("Payments", Icons.Default.Payments, "Salary"),
  CategoryIconOption("Bolt", Icons.Default.Bolt, "Utilities"),
  CategoryIconOption("DirectionsCar", Icons.Default.DirectionsCar, "Transport"),
  CategoryIconOption("LocalGasStation", Icons.Default.LocalGasStation, "Gas"),
  CategoryIconOption("Home", Icons.Default.Home, "Housing"),
  CategoryIconOption("ShoppingBag", Icons.Default.ShoppingBag, "Shopping"),
  CategoryIconOption("Movie", Icons.Default.Movie, "Entertainment"),
  CategoryIconOption("Tv", Icons.Default.Tv, "Streaming"),
  CategoryIconOption("Savings", Icons.Default.Savings, "Savings"),
  CategoryIconOption("PhoneAndroid", Icons.Default.PhoneAndroid, "Phone"),
  CategoryIconOption("Wifi", Icons.Default.Wifi, "Internet"),
  CategoryIconOption("FitnessCenter", Icons.Default.FitnessCenter, "Gym / Health"),
  CategoryIconOption("MedicalServices", Icons.Default.MedicalServices, "Medical"),
  CategoryIconOption("School", Icons.Default.School, "Education"),
  CategoryIconOption("Flight", Icons.Default.Flight, "Travel"),
  CategoryIconOption("Pets", Icons.Default.Pets, "Pets"),
  CategoryIconOption("LocalBar", Icons.Default.LocalBar, "Drinks"),
  CategoryIconOption("CardGiftcard", Icons.Default.CardGiftcard, "Gift"),
  CategoryIconOption("Work", Icons.Default.Work, "Work"),
  CategoryIconOption("Category", Icons.Default.Category, "General")
  )

  fun getIconByName(iconName: String?): ImageVector {
    if (iconName.isNullOrBlank()) return Icons.Default.Category
    return when (iconName.lowercase()) {
      "shoppingcart", "groceries", "supermarket", "super" -> Icons.Default.ShoppingCart
      "restaurant", "food", "comida", "restaurante" -> Icons.Default.Restaurant
      "payments", "salary", "salario", "sueldo", "income" -> Icons.Default.Payments
      "bolt", "utilities", "servicios", "electricidad", "luz" -> Icons.Default.Bolt
      "directionscar", "transport", "transporte", "auto", "car" -> Icons.Default.DirectionsCar
      "localgasstation", "gas", "gasolina" -> Icons.Default.LocalGasStation
      "home", "housing", "casa", "alquiler", "renta", "rent" -> Icons.Default.Home
      "shoppingbag", "shopping", "compras" -> Icons.Default.ShoppingBag
      "movie", "entertainment", "entretenimiento", "cine" -> Icons.Default.Movie
      "tv", "streaming", "netflix", "suscripciones" -> Icons.Default.Tv
      "savings", "ahorros", "ahorro" -> Icons.Default.Savings
      "phoneandroid", "phone", "telefono", "movil" -> Icons.Default.PhoneAndroid
      "wifi", "internet" -> Icons.Default.Wifi
      "fitnesscenter", "gym", "salud", "fitness" -> Icons.Default.FitnessCenter
      "medicalservices", "health", "medicina", "doctor", "farmacia" -> Icons.Default.MedicalServices
      "school", "education", "educacion", "colegio", "universidad" -> Icons.Default.School
      "flight", "travel", "viajes", "vuelo" -> Icons.Default.Flight
      "pets", "mascotas", "perro", "gato" -> Icons.Default.Pets
      "localbar", "bar", "bebidas", "fiesta" -> Icons.Default.LocalBar
      "cardgiftcard", "gift", "regalo" -> Icons.Default.CardGiftcard
      "work", "trabajo", "empleo" -> Icons.Default.Work
      "waterdrop", "agua", "water" -> Icons.Default.WaterDrop
      "localfiredepartment", "gas_dept" -> Icons.Default.LocalFireDepartment
      else -> Icons.Default.Category
    }
  }
}
