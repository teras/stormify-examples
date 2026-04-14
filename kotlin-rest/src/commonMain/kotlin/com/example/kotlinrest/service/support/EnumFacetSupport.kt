package com.example.kotlinrest.service.support

import onl.ycode.stormify.biglist.Facet
import onl.ycode.stormify.biglist.AbstractPagedQuery

/**
 * Adds an enum facet that sorts alphabetically by display name rather than
 * by the underlying ordinal stored in the database. Uses a SQL CASE
 * expression so that both sort and text filter operate on the name string.
 */
internal inline fun <reified E : Enum<E>> AbstractPagedQuery<*>.addEnumFacet(
    alias: String,
    column: String,
): Facet {
    val entries = enumValues<E>()
    val caseExpr = entries.joinToString(
        separator = " ",
        prefix = "CASE $column ",
        postfix = " END",
    ) { "WHEN ${it.ordinal} THEN '${it.name}'" }
    return addSqlFacet(alias, caseExpr, Facet.TEXT)
}
