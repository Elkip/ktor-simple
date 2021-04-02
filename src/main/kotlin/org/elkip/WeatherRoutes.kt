package org.elkip

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.weatherRoutes() {
    route("/weather") {
        route("/asia") {
            // this will only execute if the specified systemtoken is present
            header("systemtoken", "weathersystem") {
                handle {
                    call.respondText("The weather is sunny")
                }
            }
        }
        route("/europe", HttpMethod.Get) {
            // if the parameter name is not present call the other handle function
            param("name") {
                handle {
                    var name = call.parameters.get("name")
                    call.respondText("The weather is $name")
                }
            }
            handle {
                call.respondText("The weather is rainy")
            }
        }
        route("/usa") {
            get {
                call.respondText("The weather is rainy")
            }
        }
    }

    get("/goodevening") {
        call.respondText("Good evening World!")
    }
    var weather = "sunny"
    get("/weatherforecast") {
        call.respondText { weather }
    }
    post("/weatherforecast") {
        weather = call.receiveText()
        call.respondText ("The weather has been set to $weather\n", contentType = ContentType.Text.Plain)
    }
}
