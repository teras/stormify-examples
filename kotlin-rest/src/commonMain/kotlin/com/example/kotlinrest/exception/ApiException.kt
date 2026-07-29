package com.example.kotlinrest.exception

import io.ktor.http.HttpStatusCode

/**
 * Every error this API reports deliberately is one of these.
 *
 * The HTTP status travels with the exception rather than being reconstructed from the
 * error code at the edge, so the place that knows *why* something failed is the place
 * that decides how it is reported. Anything else reaching the error handler is a bug,
 * and is answered with a generic 500.
 */
sealed class ApiException(
    val status: HttpStatusCode,
    val errorCode: String,
    message: String,
) : RuntimeException(message) {

    /** Extra machine-readable context, keyed by request field where there is one. */
    open val details: Map<String, String> get() = emptyMap()
}

/** The request itself is wrong — a blank name, a negative price. */
class ValidationException(
    message: String,
    private val fieldName: String? = null,
) : ApiException(HttpStatusCode.BadRequest, "VALIDATION_ERROR", message) {
    override val details: Map<String, String>
        get() = fieldName?.let { mapOf(it to (message ?: "invalid")) } ?: emptyMap()
}

/** The id in the path names nothing. */
class EntityNotFoundException(
    entityName: String,
    entityId: Any,
) : ApiException(HttpStatusCode.NotFound, "ENTITY_NOT_FOUND", "$entityName with id $entityId was not found")

/**
 * An id *inside* the body names nothing.
 *
 * Distinct from [EntityNotFoundException] on purpose: the request reached the right
 * resource, so the resource is not missing — the body is unprocessable. 422 says that,
 * 404 would claim the endpoint itself does not exist.
 */
class ReferenceNotFoundException(
    entityName: String,
    private val fieldName: String,
    entityId: Any,
) : ApiException(
    HttpStatusCode.UnprocessableEntity,
    "REFERENCE_NOT_FOUND",
    "$entityName with id $entityId was not found",
) {
    override val details: Map<String, String> get() = mapOf(fieldName to (message ?: "not found"))
}

/**
 * The request is well formed but the current state refuses it — receiving an order that
 * is not a draft, shipping twice, deleting a row something else still points at.
 */
class ConflictException(message: String) : ApiException(HttpStatusCode.Conflict, "CONFLICT", message)
