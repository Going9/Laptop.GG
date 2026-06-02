package going9.laptopgg.job.crawler.danawa.client

import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import org.springframework.stereotype.Component

internal fun interface DanawaHttpTransport {
    @Throws(IOException::class, InterruptedException::class)
    fun send(request: HttpRequest): HttpResponse<String>
}

@Component
internal class JavaDanawaHttpTransport : DanawaHttpTransport {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    override fun send(request: HttpRequest): HttpResponse<String> {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    }
}
