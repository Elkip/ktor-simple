package org.elkip

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import java.time.LocalDateTime

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

/**
 * Please note that you can use any other name instead of *module*.
 * Also note that you can have more then one modules in your application.
 * */
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = true) {

    val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    install(ContentNegotiation) {
        // If this line is active the request will use XML, unless the header specifies otherwise
        //register(ContentType.Application.Xml, XmlConverter())
        jackson {
            registerModule(JavaTimeModule())
            enable(SerializationFeature.INDENT_OUTPUT)
            enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
        }
    }

    routing {
        trace { application.log.trace(it.buildText()) }
        weatherRoutes()

        libraryRoutes()
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }

        get("/ship"){
            call.respond(SpaceShip(null, 80, listOf("Mike")))
        }
        get("/consumeService") {
            val response = client.get<ByteArray>("http://localhost:8080/spaceship")
            log.info("the result: ${String(response)}")
        }
    }
}

data class SpaceShip(
    @JsonInclude(value=JsonInclude.Include.NON_NULL)
    val name: String?,
    val fuel: Int,
    val crew: List<String>,
    @JsonFormat(pattern="yyyy-MM-dd HH:mm")
    val launchDate: LocalDateTime = LocalDateTime.now())
