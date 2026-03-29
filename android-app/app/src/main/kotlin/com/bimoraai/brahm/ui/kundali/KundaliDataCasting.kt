package com.bimoraai.brahm.ui.kundali

internal fun Any?.asStringAnyMap(): Map<String, Any?>? =
    (this as? Map<*, *>)?.entries?.mapNotNull { (key, value) ->
        (key as? String)?.let { it to value }
    }?.toMap()

internal fun Any?.asStringAnyMapList(): List<Map<String, Any?>> =
    (this as? List<*>)?.mapNotNull { it.asStringAnyMap() } ?: emptyList()
