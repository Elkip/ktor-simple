package org.elkip

import kotlinx.coroutines.*
import kotlin.random.Random

fun main(args: Array<String>) = runBlocking {
    // 64 Threads in IO
    withContext(Dispatchers.IO) {
        repeat (100) { // 100_000 = 100,000
            launch {
                firstcoroutine(it) // it will be the current iteration
            }
        }
        println("End of withContext")
    }
    println("End of main function")
}

suspend fun firstcoroutine(id: Int) {
    delay(Random.nextLong()%2000) // The delay is a random number less than 2 seconds
    println("first $id")
}
