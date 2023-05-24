package io.javalin.community.routing.annotations

import io.javalin.http.HttpStatus
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.reflect.KClass

/**
 * Endpoints annotation is used to define a base path for all endpoints in a class.
 */
@Retention(RUNTIME)
@Target(CLASS)
annotation class Endpoints(val value: String = "")

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class ExceptionHandler(val value: KClass<out Exception>)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Get(val value: String = "", val async: Boolean = false)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Post(val value: String = "", val async: Boolean = false)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Put(val value: String = "", val async: Boolean = false)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Delete(val value: String = "", val async: Boolean = false)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Patch(val value: String = "", val async: Boolean = false)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Head(val value: String = "", val async: Boolean = false)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Options(val value: String = "", val async: Boolean = false)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Before(val value: String = "*", val async: Boolean = false)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class After(val value: String = "*", val async: Boolean = false)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Version(val value: String)

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class Status(val success: HttpStatus = HttpStatus.UNKNOWN, val error: HttpStatus = HttpStatus.UNKNOWN)

@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class Body

@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class Param(val value: String = "")

@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class Header(val value: String = "")

@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class Query(val value: String = "")

@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class Form(val value: String = "")

@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class Cookie(val value: String = "")