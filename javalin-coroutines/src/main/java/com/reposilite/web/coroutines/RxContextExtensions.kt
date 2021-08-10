package com.reposilite.web.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

import panda.std.Result

suspend fun <VALUE, ERROR, MAPPED_ERROR> Flow<Result<out VALUE, ERROR>>.firstSuccessOr(elseValue: suspend () -> Result<out VALUE, MAPPED_ERROR>): Result<out VALUE, MAPPED_ERROR> =
    this.firstOrNull { it.isOk }
        ?.projectToValue()
        ?: elseValue()

suspend fun <VALUE, ERROR> Flow<Result<out VALUE, ERROR>>.firstOrErrors(): Result<out VALUE, Collection<ERROR>> {
    val collection: MutableCollection<ERROR> = ArrayList()

    return this
        .map { result -> result.onError { collection.add(it) } }
        .firstSuccessOr { Result.error(collection) }
}