package com.example.kidskassa

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Product(val emoji: String, val name: String, val price: Int)

enum class Screen { SHOP, CASH, CARD, SUCCESS }

class SoundPlayer {
    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    } catch (e: Exception) { null }

    fun beep() { tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 80) }
    fun beepLong() { tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150) }
    fun success() { tone?.startTone(ToneGenerator.TONE_PROP_ACK, 250) }
    fun release() { try { tone?.release() } catch (_: Exception) {} }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = lightColorScheme(
                primary = Color(0xFF3D7EFF),
                secondary = Color(0xFF4CD964),
                background = Color(0xFFF2F8FF),
                surface = Color.White
            )
            MaterialTheme(colorScheme = colors) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    KidsCashierApp()
                }
            }
        }
    }
}

@Composable
fun KidsCashierApp() {
    val sound = remember { SoundPlayer() }
    DisposableEffect(Unit) { onDispose { sound.release() } }
    val scope = rememberCoroutineScope()

    val products = remember {
        listOf(
            Product("🍎", "Яблоко", 30),
            Product("🍌", "Банан", 25),
            Product("🍞", "Хлеб", 45),
            Product("🥛", "Молоко", 80),
            Product("🍫", "Шоколад", 60),
            Product("🧃", "Сок", 70),
            Product("🍪", "Печенье", 50),
            Product("🧸", "Игрушка", 150)
        )
    }

    var cart by remember { mutableStateOf<Map<Product, Int>>(emptyMap()) }
    var screen by remember { mutableStateOf(Screen.SHOP) }
    var paying by remember { mutableStateOf(false) }

    val total = cart.entries.sumOf { it.key.price * it.value }

    fun resetAll() {
        cart = emptyMap()
        screen = Screen.SHOP
        paying = false
    }

    when (screen) {
        Screen.SHOP -> ShopScreen(
            products = products,
            cart = cart,
            total = total,
            onAdd = { product ->
                sound.beep()
                cart = cart.toMutableMap().apply {
                    this[product] = (this[product] ?: 0) + 1
                }
            },
            onClear = { cart = emptyMap() },
            onCash = { screen = Screen.CASH },
            onCard = { screen = Screen.CARD }
        )

        Screen.CASH -> CashScreen(
            total = total,
            onBack = { screen = Screen.SHOP },
            onPay = {
                screen = Screen.SUCCESS
                sound.success()
            }
        )

        Screen.CARD -> CardScreen(
            total = total,
            paying = paying,
            onBack = { if (!paying) screen = Screen.SHOP },
            onTapCard = {
                if (!paying) {
                    scope.launch {
                        paying = true
                        sound.beepLong()
                        delay(160)
                        sound.beepLong()
                        delay(1400)
                        paying = false
                        screen = Screen.SUCCESS
                        sound.success()
                    }
                }
            }
        )

        Screen.SUCCESS -> SuccessScreen(
            total = total,
            onNewPurchase = { resetAll() }
        )
    }
}

@Composable
fun ShopScreen(
    products: List<Product>,
    cart: Map<Product, Int>,
    total: Int,
    onAdd: (Product) -> Unit,
    onClear: () -> Unit,
    onCash: () -> Unit,
    onCard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Касса 🛒", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (cart.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Сброс") }
            }
        }

        Text("Выбери товары", fontSize = 18.sp, color = Color(0xFF666666))
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(products, key = { it.name }) { product ->
                ProductRow(
                    product = product,
                    quantity = cart[product] ?: 0,
                    onAdd = { onAdd(product) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Итого: $total ₽", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCash,
                        enabled = cart.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800))
                    ) {
                        Text("💵 Наличные", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onCard,
                        enabled = cart.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D7EFF))
                    ) {
                        Text("💳 Картой", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductRow(product: Product, quantity: Int, onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onAdd() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(product.emoji, fontSize = 34.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${product.price} ₽", fontSize = 16.sp, color = Color(0xFF666666))
            }
            if (quantity > 0) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFFF6B9D)
                ) {
                    Text(
                        "×$quantity",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF4CD964),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CashScreen(total: Int, onBack: () -> Unit, onPay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💵", fontSize = 72.sp)
        Text("К оплате", fontSize = 20.sp, color = Color(0xFF666666))
        Text("$total ₽", fontSize = 42.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onPay,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CD964))
        ) {
            Text("Оплатить наличными", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Назад")
        }
    }
}

@Composable
fun CardScreen(
    total: Int,
    paying: Boolean,
    onBack: () -> Unit,
    onTapCard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💳", fontSize = 72.sp)
        Text("К оплате", fontSize = 20.sp, color = Color(0xFF666666))
        Text("$total ₽", fontSize = 42.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        if (paying) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color(0xFF3D7EFF),
                strokeWidth = 6.dp
            )
            Spacer(Modifier.height(12.dp))
            Text("Оплата...", fontSize = 20.sp)
        } else {
            Text("Приложи карту к терминалу", fontSize = 18.sp, color = Color(0xFF666666))
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onTapCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D7EFF))
            ) {
                Text("📱 Приложить карту", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Назад")
            }
        }
    }
}

@Composable
fun SuccessScreen(total: Int, onNewPurchase: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✅", fontSize = 96.sp)
        Text(
            "Оплата успешна!",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CD964),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text("Оплачено: $total ₽", fontSize = 22.sp)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNewPurchase,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CD964))
        ) {
            Text("Новая покупка", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
