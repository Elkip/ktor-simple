package org.elkip

import com.codahale.metrics.Slf4jReporter
import com.codahale.metrics.jmx.JmxReporter
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.locations.*
import io.ktor.metrics.dropwizard.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.slf4j.event.Level
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

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

    install(Authentication) {
        basic("myAuth1") {
            realm = " My Realm"
            validate {
                if (it.name == "mike" && it.password == "password")
                    UserIdPrincipal(it.name)
                else null
            }
        }
        basic("myAuth2") {
            realm = "MyOtherRealm"
            validate {
                if(it.password == "${it.name}abc123")
                    UserIdPrincipal(it.name)
                else
                    null
            }
        }
    }

    install(Locations) {
    }

    install(DefaultHeaders) {
        header("SystemName", "RandomApp")
    }

    install(StatusPages) {
        statusFile(HttpStatusCode.InternalServerError,
            HttpStatusCode.NotFound,
            filePattern = "customErrors/myerror#.html")
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("callLogging") }
    }

    install(DropwizardMetrics) {
        Slf4jReporter.forRegistry(registry)
            .outputTo(log)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build()
            .start(15, TimeUnit.SECONDS)

        JmxReporter.forRegistry(registry)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build()
            .start()
    }

    val mike = PipelinePhase("Mike")
    insertPhaseAfter(ApplicationCallPipeline.Call, mike)
    intercept(ApplicationCallPipeline.Setup) {
        log.info("Setup phase")
    }
    intercept(ApplicationCallPipeline.Call) {
        log.info("Call phase")
    }
    intercept(ApplicationCallPipeline.Features) {
        log.info("Features phase")
    }
    intercept(ApplicationCallPipeline.Monitoring) {
        log.info("Monitoring phase")
    }

    intercept(mike) {
        log.info("Mike Phase${call.request.uri}")
        if (call.request.uri.contains("mike")) {
            log.info("The uri contains mike")
            call.respondText("The Endpoint contains mike")
            finish()
        }
    }

    routing {
        //trace { application.log.trace(it.buildText()) }

        get("/") {
            for (h in call.request.headers.entries()) {
                log.info("header: $h")
            }
            call.response.header("MyHeader", "MyValue")
            call.response.header(HttpHeaders.SetCookie, "cookie")
            call.respondText("Hello World!\n", contentType = ContentType.Text.Plain)
        }

        get("/something/mike/something") {
            call.respondText("Endpoint handled by route.")
        }

        post("/form") {
            val params = call.receiveParameters()
            params.names().forEach {
                val myval = params.get(it)
                print("key: $it, value: $myval")
            }
            call.respondText("Thank you for this form data\n")
        }

        get("/callLogging/test") {
            call.respondText("TEST LOGGING", ContentType.Text.Plain)
        }


        authenticate("myAuth1") {
            get("/secret/weather") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name} it is secretly going to rain today")
            }
        }

        authenticate("myAuth2") {
            get("/secret/color") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}, green is going to be popular tomorrow")
            }
        }

        weatherRoutes()

        libraryRoutes()

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

        get<MyLocation> {
            call.respondText("Location: name= ${it.name}")
        }

        get<Article.Author> {
            call.respondText("$it")
        }

        get<Article.List> {
            call.respondText("$it")
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

@Location("location/{name}")
class MyLocation(val name: String)

@Location("/article/{category}")
data class Article(val category: String) {
    @Location("/{author}")
    data class Author(val article: Article, val author: String)

    @Location("/list")
    data class List(val article: Article, val sortBy: String, val asc: Int)
}
