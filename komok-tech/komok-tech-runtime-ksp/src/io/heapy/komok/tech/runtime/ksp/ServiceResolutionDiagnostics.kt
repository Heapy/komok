package io.heapy.komok.tech.runtime.ksp

internal fun serviceProviderAddress(
    moduleFqn: String,
    providerName: String,
    signatureDescriptor: String = "",
): String =
    buildString {
        append(moduleFqn)
        append('.')
        append(providerName)
        append(signatureDescriptor)
    }

internal fun missingServiceBindingMessage(
    moduleFqn: String,
    providerName: String,
    providerSignatureDescriptor: String,
    dependencyName: String,
    dependencyTypeKey: String,
): String {
    val providerAddress = serviceProviderAddress(moduleFqn, providerName, providerSignatureDescriptor)
    return "No @Service binding found for dependency `$dependencyName: $dependencyTypeKey`. " +
        "Dependency chain: $providerAddress -> $dependencyName:$dependencyTypeKey."
}

internal fun duplicateServiceBindingMessage(
    moduleFqn: String,
    providerName: String,
    providerSignatureDescriptor: String,
    dependencyName: String,
    dependencyTypeKey: String,
    candidateAddresses: List<String>,
): String {
    val providerAddress = serviceProviderAddress(moduleFqn, providerName, providerSignatureDescriptor)
    return "Multiple @Service bindings found for dependency `$dependencyName: $dependencyTypeKey`. " +
        "Dependency chain: $providerAddress -> $dependencyName:$dependencyTypeKey. " +
        "Candidates: ${candidateAddresses.joinToString()}."
}
