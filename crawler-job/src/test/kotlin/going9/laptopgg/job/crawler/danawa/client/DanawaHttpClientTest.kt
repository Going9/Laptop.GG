package going9.laptopgg.job.crawler.danawa.client

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
        val client = clientReturning(HttpResponseStub(statusCode = 200, body = "ok"))

        val response = client.send(request())

        assertThat(response).isEqualTo("ok")
    }

    @Test
    fun `send rejects non retryable status with explicit Danawa http status error`() {
        val client = clientReturning(HttpResponseStub(statusCode = 404, body = "not found"))

        assertThatThrownBy {
            client.send(request())
        }.isInstanceOf(DanawaHttpStatusException::class.java)
            .extracting("statusCode")
            .isEqualTo(404)
    }

    private fun clientReturning(response: HttpResponse<String>): DanawaHttpClient {
        return DanawaHttpClient(
            requestPacer = DanawaRequestPacer(),
            retryPolicy = DanawaRetryPolicy(),
            transport = DanawaHttpTransport { response },
        )
    }

    private fun request(): HttpRequest {
        return HttpRequest.newBuilder(URI.create("https://prod.danawa.com/list/?cate=112758")).GET().build()
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
