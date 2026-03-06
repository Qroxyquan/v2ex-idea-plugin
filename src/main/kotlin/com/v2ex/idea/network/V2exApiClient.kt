package com.v2ex.idea.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.nio.charset.StandardCharsets
import java.time.Duration

class V2exApiClient(
    private val baseUrl: String = "https://www.v2ex.com",
    private val connectTimeout: Duration = Duration.ofSeconds(5),
    private val readTimeout: Duration = Duration.ofSeconds(10),
    private val userAgent: String = "V2EX-Reader-IDEA-Plugin/0.1",
    private val tokenProvider: () -> String? = { null },
    private val a2TokenProvider: () -> String? = { null },
    private val cookieManager: CookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    },
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .cookieHandler(cookieManager)
        .build(),
) {
    suspend fun get(path: String, acceptJson: Boolean = true): String = withContext(Dispatchers.IO) {
        requestWithRetry(path, acceptJson)
    }

    suspend fun postForm(path: String, form: Map<String, String>, refererPath: String? = null): HttpResult =
        withContext(Dispatchers.IO) {
            val target = if (path.startsWith("http://") || path.startsWith("https://")) path else "$baseUrl$path"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(target))
                .timeout(readTimeout)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .apply {
                    val a2 = a2TokenProvider()?.trim().orEmpty()
                    if (a2.isNotBlank()) {
                        header("Cookie", "A2=$a2")
                    }
                    if (!refererPath.isNullOrBlank()) {
                        header("Referer", if (refererPath.startsWith("http")) refererPath else "$baseUrl$refererPath")
                    }
                }
                .POST(HttpRequest.BodyPublishers.ofString(urlEncodeForm(form)))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            HttpResult(code = response.statusCode(), body = response.body())
        }

    private fun requestWithRetry(path: String, acceptJson: Boolean): String {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                val hasToken = !tokenProvider().isNullOrBlank()
                val response = execute(path, acceptJson, includeToken = true)
                val code = response.statusCode()
                if (code in 200..299) {
                    return response.body()
                }
                if (hasToken && (code == 401 || code == 403)) {
                    val anonymous = execute(path, acceptJson, includeToken = false)
                    if (anonymous.statusCode() in 200..299) {
                        return anonymous.body()
                    }
                }
                if (code >= 500 && attempt == 0) {
                    Thread.sleep(400)
                    return@repeat
                }
                throw IOException("请求失败：HTTP $code")
            } catch (err: HttpTimeoutException) {
                lastError = err
                if (attempt == 0) {
                    Thread.sleep(400)
                }
            } catch (err: IOException) {
                lastError = err
                if (attempt == 0) {
                    Thread.sleep(400)
                }
            }
        }

        throw IOException("请求失败：${lastError?.message ?: "未知错误"}", lastError)
    }

    private fun execute(path: String, acceptJson: Boolean, includeToken: Boolean): HttpResponse<String> {
        val target = withToken(
            if (path.startsWith("http://") || path.startsWith("https://")) path else "$baseUrl$path",
            includeToken = includeToken,
        )
        val request = HttpRequest.newBuilder()
            .uri(URI.create(target))
            .timeout(readTimeout)
            .header("User-Agent", userAgent)
            .header("Accept", if (acceptJson) "application/json" else "text/html,application/xhtml+xml")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .apply {
                val a2 = a2TokenProvider()?.trim().orEmpty()
                if (a2.isNotBlank()) {
                    header("Cookie", "A2=$a2")
                }
            }
            .GET()
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun withToken(target: String, includeToken: Boolean): String {
        if (!target.contains("/api/")) return target
        if (!includeToken) return target
        if (target.contains("token=")) return target
        val token = tokenProvider()?.trim().orEmpty()
        if (token.isBlank()) return target
        val separator = if (target.contains("?")) "&" else "?"
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8)
        return "$target${separator}token=$encodedToken"
    }

    private fun urlEncodeForm(form: Map<String, String>): String {
        return form.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, StandardCharsets.UTF_8)}=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
        }
    }

    data class HttpResult(val code: Int, val body: String)
}
