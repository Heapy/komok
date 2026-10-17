package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonElement

/**
 * DSL builder for [Contact] object.
 *
 * Example usage:
 * ```kotlin
 * val contact = contact {
 *     name = "API Support"
 *     url = "https://example.com/support"
 *     email = "support@example.com"
 * }
 * ```
 */
class ContactBuilder {
    var name: String? = null
    var url: String? = null
    var email: String? = null
    var extensions: Map<String, JsonElement>? = null

    fun build(): Contact = Contact(
        name = name,
        url = url,
        email = email,
        extensions = extensions,
    )
}

/**
 * Creates a [Contact] object using DSL syntax.
 *
 * @param block configuration block for the contact
 * @return configured Contact object
 */
inline fun contact(block: ContactBuilder.() -> Unit): Contact {
    return ContactBuilder().apply(block).build()
}

/**
 * DSL builder for [License] object.
 *
 * The builder enforces fail-fast validation:
 * - `name` is required
 * - `identifier` and `url` are mutually exclusive
 *
 * Example usage:
 * ```kotlin
 * val license = license {
 *     name = "Apache 2.0"
 *     identifier = "Apache-2.0"
 * }
 * ```
 */
class LicenseBuilder {
    var name: String? = null
    var identifier: String? = null
    var url: String? = null
    var extensions: Map<String, JsonElement>? = null

    fun build(): License {
        val licenseName = requireNotNull(name) {
            "License name is required"
        }
        require(identifier == null || url == null) {
            "License identifier and url are mutually exclusive. Only one should be specified."
        }
        return License(
            name = licenseName,
            identifier = identifier,
            url = url,
            extensions = extensions,
        )
    }
}

/**
 * Creates a [License] object using DSL syntax.
 *
 * @param block configuration block for the license
 * @return configured License object
 * @throws IllegalArgumentException if name is not provided
 * @throws IllegalArgumentException if both identifier and url are provided
 */
inline fun license(block: LicenseBuilder.() -> Unit): License {
    return LicenseBuilder().apply(block).build()
}

/**
 * DSL builder for [ExternalDocumentation] object.
 *
 * The builder enforces fail-fast validation:
 * - `url` is required
 *
 * Example usage:
 * ```kotlin
 * val externalDocs = externalDocumentation {
 *     description = "Find more info here"
 *     url = "https://example.com/docs"
 * }
 * ```
 */
class ExternalDocumentationBuilder {
    var description: String? = null
    var url: String? = null
    var extensions: Map<String, JsonElement>? = null

    fun build(): ExternalDocumentation {
        val documentationUrl = requireNotNull(url) {
            "ExternalDocumentation url is required"
        }
        return ExternalDocumentation(
            description = description,
            url = documentationUrl,
            extensions = extensions,
        )
    }
}

/**
 * Creates an [ExternalDocumentation] object using DSL syntax.
 *
 * @param block configuration block for the external documentation
 * @return configured ExternalDocumentation object
 * @throws IllegalArgumentException if url is not provided
 */
inline fun externalDocumentation(block: ExternalDocumentationBuilder.() -> Unit): ExternalDocumentation {
    return ExternalDocumentationBuilder().apply(block).build()
}

/**
 * DSL builder for [Tag] object.
 *
 * The builder enforces fail-fast validation:
 * - `name` is required
 *
 * Supports nested DSL for external documentation:
 * ```kotlin
 * val tag = tag {
 *     name = "pets"
 *     description = "Everything about your Pets"
 *     externalDocs {
 *         url = "https://example.com/pets"
 *     }
 * }
 * ```
 */
class TagBuilder {
    var name: String? = null
    var summary: String? = null
    var description: String? = null
    var externalDocs: ExternalDocumentation? = null
    var parent: String? = null
    var kind: String? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures external documentation using DSL syntax.
     */
    inline fun externalDocs(block: ExternalDocumentationBuilder.() -> Unit) {
        externalDocs = externalDocumentation(block)
    }

    fun build(): Tag {
        val tagName = requireNotNull(name) {
            "Tag name is required"
        }
        return Tag(
            name = tagName,
            summary = summary,
            description = description,
            externalDocs = externalDocs,
            parent = parent,
            kind = kind,
            extensions = extensions,
        )
    }
}

/**
 * Creates a [Tag] object using DSL syntax.
 *
 * @param block configuration block for the tag
 * @return configured Tag object
 * @throws IllegalArgumentException if name is not provided
 */
inline fun tag(block: TagBuilder.() -> Unit): Tag {
    return TagBuilder().apply(block).build()
}

/**
 * Creates a list of [Tag] objects using DSL syntax.
 *
 * Example usage:
 * ```kotlin
 * val tags = tags {
 *     tag {
 *         name = "pets"
 *         description = "Pet operations"
 *     }
 *     tag {
 *         name = "users"
 *         description = "User operations"
 *     }
 * }
 * ```
 */
class TagsBuilder {
    @PublishedApi
    internal val tags = mutableListOf<Tag>()

    /**
     * Adds a tag using DSL syntax.
     */
    inline fun tag(block: TagBuilder.() -> Unit) {
        tags.add(io.heapy.komok.tech.api.dsl.tag(block))
    }

    /**
     * Adds a pre-built tag.
     */
    fun tag(tag: Tag) {
        tags.add(tag)
    }

    fun build(): List<Tag> = tags.toList()
}

/**
 * Creates a list of [Tag] objects using DSL syntax.
 *
 * @param block configuration block for the tags
 * @return list of configured Tag objects
 */
inline fun tags(block: TagsBuilder.() -> Unit): List<Tag> {
    return TagsBuilder().apply(block).build()
}
