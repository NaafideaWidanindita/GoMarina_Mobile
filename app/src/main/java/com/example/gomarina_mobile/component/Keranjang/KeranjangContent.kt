@file:Suppress("DEPRECATION")

package com.example.gomarina_mobile.component.Keranjang

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gomarina_mobile.R
import com.example.gomarina_mobile.dummyData.DummyData
import com.example.gomarina_mobile.model.KeranjangItem
import com.example.gomarina_mobile.ui.theme.*
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.RoundingMode
import kotlin.math.roundToInt

var selectedItems by mutableStateOf<List<Map<String, Any>>>(emptyList())

@Composable
fun KeranjangContent(
    navController: NavHostController,
    items: List<KeranjangItem>
) {
    var itemList by remember { mutableStateOf(items.toMutableList()) }
    var total by remember { mutableStateOf(0) }
    var isAllChecked by remember { mutableStateOf(false) }

    LaunchedEffect(itemList) {
        total = itemList.filter { it.isChecked }
            .sumOf {
                it.price.multiply(it.jumlah.toBigDecimal())
            }
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
    }

    fun updateSelectedItems(item: KeranjangItem, isChecked: Boolean) {
        val updatedList = selectedItems.toMutableList()
        if (isChecked) {
            updatedList.add(
                mapOf(
                    "id" to item.id,
                    "name" to item.name,
                    "jumlah" to item.jumlah,
                    "price" to item.price
                )
            )
        } else {
            updatedList.removeAll { it["id"] == item.id.toString() }
        }
        selectedItems = updatedList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bacground)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(itemList) { item ->
                SwipeableItemKeranjang(
                    item = item,
                    onQuantityChange = { newQuantity ->
                        val updatedList = itemList.map {
                            if (it.id == item.id) it.copy(jumlah = newQuantity) else it
                        }.toMutableList()
                        itemList = updatedList
                    },
                    onDelete = {
                        val updatedList = itemList.filterNot { it.id == item.id }
                        itemList = updatedList.toMutableList()
                    },
                    onCheckChange = { isChecked ->
                        val updatedList = itemList.map {
                            if (it.id == item.id) it.copy(isChecked = isChecked) else it
                        }.toMutableList()
                        itemList = updatedList
                        updateSelectedItems(item, isChecked)
                        Log.d("Item_Satuan", "Items updated: $selectedItems")
                    }
                )
            }
        }

        ButtonCheckout(
            navController = navController,
            total = total,
            itemList = itemList,
            onAllCheckedChange = { checked ->
                itemList = itemList.map { it.copy(isChecked = checked) }.toMutableList()
                isAllChecked = checked
                // Update selectedItems untuk semua item
                selectedItems = if (checked) {
                    Log.d("ALL_ITEM", "ALLL Items: $selectedItems")
                    itemList.map {
                        mapOf(
                            "id" to it.id,
                            "name" to it.name,
                            "jumlah" to it.jumlah,
                            "price" to it.price
                        )
                    }.toMutableList()
                } else {
                    mutableListOf() // Kosongkan selectedItems jika tidak ada yang dicentang
                }
            },
            onItemCheckedChange = { index, checked ->
                // Update item isChecked
                itemList = itemList.mapIndexed { i, item ->
                    if (i == index) item.copy(isChecked = checked) else item
                }.toMutableList()
                // Update selectedItems
                updateSelectedItems(itemList[index], checked)
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableItemKeranjang(
    item: KeranjangItem,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onCheckChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Fungsi untuk menghapus item dari cart menggunakan OkHttp
    fun hapusItem(id: Int) {
        hapusItemFromCart(id) {
            // Setelah item dihapus, panggil onDelete untuk mengupdate UI
            onDelete()
            Toast.makeText(context, "Item berhasil dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    val swipeState = rememberSwipeableState(initialValue = 0)
    val maxSwipeOffset = 100.dp
    val anchors = mapOf(0f to 0, -with(LocalDensity.current) { maxSwipeOffset.toPx() } to 1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .swipeable(
                state = swipeState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
    ) {
        if (swipeState.currentValue == 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(onClick = { hapusItem(item.id) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .offset { IntOffset(swipeState.offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            colors = CardDefaults.cardColors(bg_card),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onCheckChange(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = button,
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.White
                    )
                )
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl.replace("localhost", "10.0.2.2"))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Gambar ${item.name}",
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Rp${item.price}",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (item.jumlah > 1) onQuantityChange(item.jumlah - 1) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.White, shape = CircleShape)
                                .border(2.dp, button, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "-", fontSize = 30.sp, color = Color.Black)
                        }
                    }
                    Text(
                        text = "${item.jumlah}kg",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    )
                    IconButton(
                        onClick = { onQuantityChange(item.jumlah + 1) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(button, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "+", fontSize = 20.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ButtonCheckout(
    navController: NavHostController,
    total: Int,
    itemList: List<KeranjangItem>,
    onAllCheckedChange: (Boolean) -> Unit,
    onItemCheckedChange: (Int, Boolean) -> Unit
) {
    var isAllChecked by remember { mutableStateOf(false) }

    LaunchedEffect(itemList) {
        isAllChecked = itemList.all { it.isChecked }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .navigationBarsPadding()
            .background(bg_button)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isAllChecked,
                    onCheckedChange = { checked ->
                        // jika semua di klik maka semua akan tercentang
                        isAllChecked = checked
                        itemList.forEachIndexed { index, item ->
                            onItemCheckedChange(index, checked)
                        }
                        onAllCheckedChange(checked)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = button,
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "Semua",
                    fontFamily = poppinsFamily,
                    fontSize = 15.sp,
                    color = Color(0xFF666464),
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "Total Rp$total",
                fontFamily = poppinsFamily,
                fontSize = 15.sp,
                color = Color(0xFF666464),
                fontWeight = FontWeight.Medium
            )
            fun convertToJson(): String {
                val gson = Gson()
                return gson.toJson(selectedItems)
            }
            Button(
                onClick = {
                    val jsonData = convertToJson()
                    // Mengirim jsonData yang sudah terformat JSON
                    navController.navigate("pesanan/$jsonData")
                    Log.d("Yang dikirim", jsonData)
                },
                colors = ButtonDefaults.buttonColors(containerColor = button),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(150.dp)
            ) {
                Text(
                    text = "Checkout ($total)",
                    color = Color.White,
                    fontFamily = poppinsFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

fun hapusItemFromCart(id: Int, onDelete: () -> Unit) {
    // Membuat request HTTP DELETE
    val client = OkHttpClient()

    val request = Request.Builder()
        .url("http://10.0.2.2:5000/api/v1/cart_item/$id")  // Ganti dengan URL API kamu
        .delete()  // Menggunakan metode DELETE
        .build()

    // Menjalankan request di background thread
    Thread {
        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                // Jika berhasil, panggil onDelete untuk mengupdate UI
                onDelete()
                Log.d("DeleteCart", "Item berhasil dihapus")
            } else {
                Log.d("DeleteCart", "Gagal menghapus item: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e("DeleteCart", "Error: ${e.message}")
        }
    }.start()
}

@Preview(showBackground = true)
@Composable
fun KeranjangPreview() {
    val navController = rememberNavController()
    KeranjangContent(
        navController = navController,
        items = DummyData.dataKeranjangItems
    )
}
