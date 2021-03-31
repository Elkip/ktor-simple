package org.elkip

fun Int.addFive(): Int {
    return this + 5
}

fun main(args: Array<String>) {
    val myInt = 5
    println(myInt.addFive())
}
