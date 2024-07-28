package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

fun interface PropertySelector {
    fun test(property: KSPropertyDeclaration): Boolean
}

val publicOpenProperty = PropertySelector { property ->
    property.isPublic() && property.isOpen()
}
