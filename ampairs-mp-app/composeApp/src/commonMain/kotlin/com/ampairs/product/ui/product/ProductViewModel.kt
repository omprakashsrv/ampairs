package com.ampairs.product.ui.product

import androidx.lifecycle.ViewModel
import com.ampairs.product.domain.Product

class ProductViewModel : ViewModel() {
    var cartProducts: MutableList<Product> = mutableListOf()
    var onProductQtyChangeCallback: (List<Product>) -> Unit = {}
    fun onProductQtyChange() {
        onProductQtyChangeCallback(cartProducts)
    }
}