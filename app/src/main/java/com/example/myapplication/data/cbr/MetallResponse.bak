package com.example.myapplication.data.cbr

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "Metall", strict = false)
data class MetallResponse(
    @field:ElementList(name = "Record", inline = true, required = false)
    var records: List<MetallRecord>? = emptyList()
)

@Root(name = "Record", strict = false)
data class MetallRecord(
    @field:Attribute(name = "Date", required = false)
    var date: String? = null,

    // Code: 1 — золото, 2 — серебро, 3 — платина, 4 — палладий
    @field:Attribute(name = "Code", required = false)
    var code: Int? = null,

    // в XML у ЦБ теги <Buy> и <Sell> (значения с запятой и пробелами)
    @field:Element(name = "Buy", required = false)
    var buy: String? = null,

    @field:Element(name = "Sell", required = false)
    var sell: String? = null
)
