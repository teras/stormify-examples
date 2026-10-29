package com.example.kotlinrest.service.inventory

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.inventory.StockListItemResponse
import com.example.kotlinrest.dto.inventory.toListItemResponse
import com.example.kotlinrest.entity.StockItem
import com.example.kotlinrest.service.support.PagedQuerySupport
import com.example.kotlinrest.service.support.CsvSupport
import onl.ycode.stormify.biglist.Facet
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.Converter
import onl.ycode.stormify.coroutines.SuspendStormify

internal class StockService(private val async: SuspendStormify) {
    private val query = async.stormify.pagedQuery<StockItem>().apply {
        val product = addTableRef("product")
        addFacet("search", "warehouse.name", "product.sku", "product.name").also { it.isSortable = false }
        addFacet("warehouseName", "warehouse.name")
        addFacet("productName", "product.name")
        addFacet("productSku", "product.sku")
        addFacet("warehouseId", "warehouse.id")
        addFacet("productId", "product.id")
        addFacet("quantityOnHand", "quantityOnHand")
        addFacet("quantityReserved", "quantityReserved")
        addFacet("reorderLevel", "product.reorderLevel")
        addFacet("lastUpdatedAt", "lastUpdatedAt")
        addSqlFacet(
            "availableQuantity",
            "(stock_item.quantity_on_hand - stock_item.quantity_reserved)",
            Facet.Type.NUMERIC
        )
        addSqlFacet(
            "belowReorder",
            "CASE WHEN stock_item.quantity_on_hand <= ${product}.reorder_level THEN 1 ELSE 0 END",
            Facet.Type.NUMERIC
        ).also { facet ->
            facet.isSortable = false
            facet.converter = Converter { column, input, _, args ->
                val expected = when (input.trim().lowercase()) {
                    "true", "1", "yes" -> 1
                    "false", "0", "no" -> 0
                    else -> throw IllegalArgumentException("Facet 'belowReorder' expects true/false")
                }
                args.add(expected)
                "$column = ?"
            }
        }
    }

    suspend fun search(spec: PageSpec): PagedResponse<StockListItemResponse> = async.withConnection {
        val page = query.execute(PagedQuerySupport.searchSpec(query, spec, defaultSortAlias = "warehouseName"))
        PagedQuerySupport.buildResponse(page) { it.toListItemResponse() }
    }

    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (StockListItemResponse) -> Any?>>(
            "id" to { it.id },
            "warehouseName" to { it.warehouseName },
            "productSku" to { it.productSku },
            "productName" to { it.productName },
            "quantityOnHand" to { it.quantityOnHand },
            "quantityReserved" to { it.quantityReserved },
            "availableQuantity" to { it.availableQuantity },
            "reorderLevel" to { it.reorderLevel },
            "lastUpdatedAt" to { it.lastUpdatedAt },
        )
        writeLine(CsvSupport.header(columns))
        // forEachStreaming walks the whole result set, so the pagination fields of the
        // spec are ignored on this path: page 0 with a large page size.
        query.forEachStreaming(spec.copy(page = 0, pageSize = 1000)) { writeLine(CsvSupport.row(columns, it.toListItemResponse())) }
    }
}
