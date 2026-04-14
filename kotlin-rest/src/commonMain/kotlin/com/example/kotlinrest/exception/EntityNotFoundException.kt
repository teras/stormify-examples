package com.example.kotlinrest.exception

class EntityNotFoundException(
    entityName: String,
    entityId: Int,
) : ApiException("$entityName with id $entityId was not found", "ENTITY_NOT_FOUND")
