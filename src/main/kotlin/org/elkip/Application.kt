package org.elkip

import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.gson.*

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
        gson {
        }
    }

    routing {
        // trace { application.log.trace(it.buildText()) }
        weatherRoutes()

        libraryRoutes()

        get("/spaceship") {
            call.respond(Spaceship("myspaceship", 15))
        }
        get("/consumeService") {
            val response = client.get<ByteArray>("http://localhost:8080/spaceship")
            log.info("the result: ${String(response)}")
        }
    }
}

data class Spaceship(val name: String, val fuel: Int)

