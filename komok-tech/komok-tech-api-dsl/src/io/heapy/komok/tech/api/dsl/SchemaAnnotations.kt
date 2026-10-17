package io.heapy.komok.tech.api.dsl

// ============================================
// Numeric Constraints
// ============================================

/**
 * Specifies the minimum value for a numeric property.
 * Maps to JSON Schema "minimum" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Min(val value: Long)

/**
 * Specifies the maximum value for a numeric property.
 * Maps to JSON Schema "maximum" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Max(val value: Long)

/**
 * Specifies the exclusive minimum value for a numeric property.
 * Maps to JSON Schema "exclusiveMinimum" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExclusiveMin(val value: Long)

/**
 * Specifies the exclusive maximum value for a numeric property.
 * Maps to JSON Schema "exclusiveMaximum" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExclusiveMax(val value: Long)

/**
 * Specifies that a numeric property must be positive (> 0).
 * Equivalent to @ExclusiveMin(0).
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Positive

/**
 * Specifies that a numeric property must be negative (< 0).
 * Equivalent to @ExclusiveMax(0).
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Negative

/**
 * Specifies that a numeric property must be positive or zero (>= 0).
 * Equivalent to @Min(0).
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class PositiveOrZero

/**
 * Specifies that a numeric property must be negative or zero (<= 0).
 * Equivalent to @Max(0).
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class NegativeOrZero

/**
 * Specifies that a numeric value must be a multiple of the given value.
 * Maps to JSON Schema "multipleOf" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class MultipleOf(val value: Double)

// ============================================
// String Constraints
// ============================================

/**
 * Specifies the minimum length for a string property.
 * Maps to JSON Schema "minLength" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SchemaMinLength(val value: Int)

/**
 * Specifies the maximum length for a string property.
 * Maps to JSON Schema "maxLength" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SchemaMaxLength(val value: Int)

/**
 * Specifies that a string property must not be empty.
 * Equivalent to @SchemaMinLength(1).
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotEmpty

/**
 * Specifies that a string property must not be blank (not empty and not only whitespace).
 * Adds minLength(1) and a pattern constraint.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotBlank

/**
 * Specifies a regex pattern for a string property.
 * Maps to JSON Schema "pattern" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SchemaPattern(val regex: String)

// ============================================
// Format Hints
// ============================================

/**
 * Specifies that a string property is an email address.
 * Maps to JSON Schema format: "email".
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Email

/**
 * Specifies that a string property is a URL/URI.
 * Maps to JSON Schema format: "uri".
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Url

/**
 * Specifies that a string property is a UUID.
 * Maps to JSON Schema format: "uuid".
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Uuid

/**
 * Specifies that a string property is a date (ISO 8601).
 * Maps to JSON Schema format: "date".
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Date

/**
 * Specifies that a string property is a date-time (ISO 8601).
 * Maps to JSON Schema format: "date-time".
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DateTime

/**
 * Specifies a custom format for a property.
 * Maps to JSON Schema "format" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SchemaFormat(val value: String)

// ============================================
// Array/Collection Constraints
// ============================================

/**
 * Specifies the minimum number of items in an array property.
 * Maps to JSON Schema "minItems" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SchemaMinItems(val value: Int)

/**
 * Specifies the maximum number of items in an array property.
 * Maps to JSON Schema "maxItems" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SchemaMaxItems(val value: Int)

/**
 * Specifies that array items must be unique.
 * Maps to JSON Schema "uniqueItems" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class UniqueItems

// ============================================
// Documentation
// ============================================

/**
 * Provides a title for the schema.
 * Maps to JSON Schema "title" keyword.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SchemaTitle(val value: String)

/**
 * Provides a description for the schema.
 * Maps to JSON Schema "description" keyword.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SchemaDescription(val value: String)

// ============================================
// Default Value
// ============================================

/**
 * Specifies a default value for a string property.
 * Maps to JSON Schema "default" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultString(val value: String)

/**
 * Specifies a default value for a numeric property.
 * Maps to JSON Schema "default" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultNumber(val value: Double)

/**
 * Specifies a default value for a boolean property.
 * Maps to JSON Schema "default" keyword.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultBoolean(val value: Boolean)
