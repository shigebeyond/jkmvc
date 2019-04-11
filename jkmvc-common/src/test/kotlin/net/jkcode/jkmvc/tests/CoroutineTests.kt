package net.jkcode.jkmvc.tests

//import kotlinx.coroutines.experimental.*

class CoroutineTests{

    /*@Test
    fun testCoroutine(){
        val mainThread = Thread.currentThread()
        println("Start")

        // Start a coroutine
        launch(CommonPool) {
            val coroutineThead = Thread.currentThread()
            println("Same Thread: " + (coroutineThead === mainThread))

            delay(1000)
            println("Hello")
        }

        Thread.sleep(2000) // wait for 2 seconds
        println("Stop")
    }

    @Test
    fun testAsync(){
        val deferred = (1..10).map { n ->
            async (CommonPool) {
                n
            }
        }
        println(deferred)

    }

    suspend fun doSomething(): Int {
        return 10;
    }*/
}