package org.elkip

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
import io.ktor.request.*
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


        post("/form") {
            val params = call.receiveParameters()
            params.names().forEach {
                val myval = params.get(it)
                print("key: $it, value: $myval")
            }
            call.respondText("Thank you for this form data\n")
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
