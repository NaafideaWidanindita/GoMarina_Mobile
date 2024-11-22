package com.example.gomarina_mobile.model

import java.math.BigDecimal

data class Produk(
    val id: Int,
    val name: String,
    val image: Int,
    val description: String,
    val price: BigDecimal,
    val stok: Int
)