package going9.laptopgg.job.crawler.danawa.client

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Optional
import javax.net.ssl.SSLSession
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DanawaHttpClientTest {
    @Test
    fun `send returns response body for successful status`() {
        val client = clientWith(SequencedDanawaHttpTransport(HttpResponseStub(statusCode = 200, body = "ok")))

        val response = client.send(request())

        assertThat(response).isEqualTo("ok")
    }

    @Test
    fun `send rejects non retryable status with explicit Danawa http status error`() {
        val client = clientWith(SequencedDanawaHttpTransport(HttpResponseStub(statusCode = 404, body = "not found")))

        assertThatThrownBy {
            client.send(request())
        }.isInstanceOf(DanawaHttpStatusException::class.java)
            .extracting("statusCode")
            .isEqualTo(404)
    }

    @Test
    fun `send retries retryable status after cooldown and returns later success`() {
        val clock = MutableDanawaClock(currentTimeMillis = 1_000L)
        val sleeper = RecordingDanawaSleeper(clock)
        val transport = SequencedDanawaHttpTransport(
            HttpResponseStub(statusCode = 503, body = "try later"),
            HttpResponseStub(statusCode = 200, body = "ok"),
        )
        val client = clientWith(transport, clock, sleeper)

        val response = client.send(request())

        assertThat(response).isEqualTo("ok")
        assertThat(transport.requestCount).isEqualTo(2)
        assertThat(sleeper.sleptMillis).containsExactly(1_500L)
    }

    @Test
    fun `send retries io failures and keeps final io cause`() {
        val clock = MutableDanawaClock(currentTimeMillis = 1_000L)
        val sleeper = RecordingDanawaSleeper(clock)
        val transport = ThrowingDanawaHttpTransport { attempt -> IOException("io-$attempt") }
        val client = clientWith(transport, clock, sleeper)

        assertThatThrownBy {
            client.send(request())
        }.isInstanceOf(DanawaHttpRequestException::class.java)
            .cause()
            .isInstanceOf(IOException::class.java)
            .hasMessage("io-3")

        assertThat(transport.requestCount).isEqualTo(3)
        assertThat(sleeper.sleptMillis).containsExactly(800L, 1_600L)
    }

    @Test
    fun `send wraps interrupted transport failures and restores interrupt flag`() {
        val interrupted = InterruptedException("stop")
        val client = clientWith(DanawaHttpTransport { throw interrupted })

        try {
            assertThatThrownBy {
                client.send(request())
            }.isInstanceOf(DanawaHttpInterruptedException::class.java)
                .cause()
                .isSameAs(interrupted)
            assertThat(Thread.currentThread().isInterrupted).isTrue()
        } finally {
            Thread.interrupted()
        }
    }

    private fun clientWith(
        transport: DanawaHttpTransport,
        clock: MutableDanawaClock = MutableDanawaClock(currentTimeMillis = 1_000L),
        sleeper: DanawaSleeper = RecordingDanawaSleeper(clock),
    ): DanawaHttpClient {
        return DanawaHttpClient(
            requestPacer = DanawaRequestPacer(
                clock = clock,
                sleeper = sleeper,
                jitterSource = FixedDanawaJitterSource(jitterMillis = 0L),
            ),
            retryPolicy = DanawaRetryPolicy(FixedDanawaJitterSource(jitterMillis = 0L)),
            transport = transport,
        )
    }

    private fun request(): HttpRequest {
        return HttpRequest.newBuilder(URI.create("https://prod.danawa.com/list/?cate=112758")).GET().build()
    }

    private class MutableDanawaClock(
        var currentTimeMillis: Long,
    ) : DanawaClock {
        override fun currentTimeMillis(): Long {
            return currentTimeMillis
        }
    }

    private class RecordingDanawaSleeper(
        private val clock: MutableDanawaClock,
    ) : DanawaSleeper {
        val sleptMillis = mutableListOf<Long>()

        override fun sleep(waitMillis: Long) {
            sleptMillis += waitMillis
            clock.currentTimeMillis += waitMillis
        }
    }

    private class FixedDanawaJitterSource(
        private val jitterMillis: Long,
    ) : DanawaJitterSource {
        override fun nextLong(maxInclusive: Long): Long {
            return jitterMillis.coerceAtMost(maxInclusive)
        }
    }

    private class SequencedDanawaHttpTransport(
        private vararg val responses: HttpResponse<String>,
    ) : DanawaHttpTransport {
        var requestCount = 0
            private set

        override fun send(request: HttpRequest): HttpResponse<String> {
            requestCount++
            return responses[requestCount - 1]
        }
    }

    private class ThrowingDanawaHttpTransport(
        private val exceptionFactory: (attempt: Int) -> IOException,
    ) : DanawaHttpTransport {
        var requestCount = 0
            private set

        override fun send(request: HttpRequest): HttpResponse<String> {
            requestCount++
            throw exceptionFactory(requestCount)
        }
    }

    private class HttpResponseStub(
        private val statusCode: Int,
        private val body: String,
    ) : HttpResponse<String> {
        override fun statusCode(): Int = statusCode
        override fun request(): HttpRequest = HttpRequest.newBuilder(URI.create("https://prod.danawa.com")).GET().build()
        override fun previousResponse(): Optional<HttpResponse<String>> = Optional.empty()
        override fun headers(): HttpHeaders = HttpHeaders.of(emptyMap()) { _, _ -> true }
        override fun body(): String = body
        override fun sslSession(): Optional<SSLSession> = Optional.empty()
        override fun uri(): URI = URI.create("https://prod.danawa.com")
        override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
    }
}
