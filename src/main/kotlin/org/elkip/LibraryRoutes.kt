package org.elkip

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.libraryRoutes() {
    get("/library/book/{bookid}/checkout") {
        val bookid = call.parameters.get("bookid")
        call.respondText("You checked out the book with id $bookid")
    }

    get("/library/book/{bookid}/reserve") {
        val bookid = call.parameters.get("bookid")
        call.respondText("You reserved the book with id $bookid")
    }

    get("/library/book/{bookid}") {
        val bookid = call.parameters.get("bookid")
        val book = Book(bookid!!, "How to grow potatoes", "La Pom de Terr")
        val hypermediaLinks = listOf<HypermediaLinks>(
            HypermediaLinks("http://localhost:8080/library/book/$bookid/checkout", "checkout", "GET"),
            HypermediaLinks("http://localhost:8080/library/book/$bookid/reserve", "reserve", "GET")
        )
        val bookResponse = BookResponse(book, hypermediaLinks)
        call.respond(bookResponse)
    }
}

data class Book(val id: String, val title: String, val author: String)
data class BookResponse(val book: Book, val links: List<HypermediaLinks>)
data class HypermediaLinks(val href: String, val rel: String, val type: String)
