package com.example.kotlinrest.mapper

import com.example.kotlinrest.dto.masterdata.CategoryDetailsResponse
import com.example.kotlinrest.dto.masterdata.CategoryListItemResponse
import com.example.kotlinrest.dto.masterdata.CustomerDetailsResponse
import com.example.kotlinrest.dto.masterdata.CustomerListItemResponse
import com.example.kotlinrest.dto.masterdata.ProductDetailsResponse
import com.example.kotlinrest.dto.masterdata.ProductListItemResponse
import com.example.kotlinrest.dto.masterdata.ReferenceSummaryResponse
import com.example.kotlinrest.dto.masterdata.SupplierDetailsResponse
import com.example.kotlinrest.dto.masterdata.SupplierListItemResponse
import com.example.kotlinrest.dto.masterdata.WarehouseDetailsResponse
import com.example.kotlinrest.dto.masterdata.WarehouseListItemResponse
import com.example.kotlinrest.entity.Category
import com.example.kotlinrest.entity.Customer
import com.example.kotlinrest.entity.Product
import com.example.kotlinrest.entity.Supplier
import com.example.kotlinrest.entity.Warehouse

fun Category.toListItemResponse() = CategoryListItemResponse(
    id = id ?: 0,
    name = name,
    description = description,
    active = active,
)

fun Category.toDetailsResponse() = CategoryDetailsResponse(
    id = id ?: 0,
    name = name,
    description = description,
    active = active,
)

fun Supplier.toListItemResponse() = SupplierListItemResponse(
    id = id ?: 0,
    name = name,
    contactName = contactName,
    city = city,
    country = country,
    active = active,
)

fun Supplier.toDetailsResponse() = SupplierDetailsResponse(
    id = id ?: 0,
    name = name,
    contactName = contactName,
    email = email,
    phone = phone,
    city = city,
    country = country,
    active = active,
)

fun Customer.toListItemResponse() = CustomerListItemResponse(
    id = id ?: 0,
    name = name,
    email = email,
    city = city,
    country = country,
    customerType = customerType,
    active = active,
)

fun Customer.toDetailsResponse() = CustomerDetailsResponse(
    id = id ?: 0,
    name = name,
    email = email,
    phone = phone,
    city = city,
    country = country,
    customerType = customerType,
    active = active,
)

fun Warehouse.toListItemResponse() = WarehouseListItemResponse(
    id = id ?: 0,
    code = code,
    name = name,
    city = city,
    country = country,
    active = active,
)

fun Warehouse.toDetailsResponse() = WarehouseDetailsResponse(
    id = id ?: 0,
    code = code,
    name = name,
    city = city,
    country = country,
    active = active,
)

fun Product.toListItemResponse() = ProductListItemResponse(
    id = id ?: 0,
    sku = sku,
    name = name,
    categoryId = category?.id,
    categoryName = category?.name,
    supplierId = supplier?.id,
    supplierName = supplier?.name,
    unitPrice = unitPrice,
    active = active,
)

fun Product.toDetailsResponse() = ProductDetailsResponse(
    id = id ?: 0,
    sku = sku,
    name = name,
    description = description,
    category = category?.let { ReferenceSummaryResponse(it.id ?: 0, it.name) },
    supplier = supplier?.let { ReferenceSummaryResponse(it.id ?: 0, it.name) },
    unitPrice = unitPrice,
    reorderLevel = reorderLevel,
    active = active,
)
