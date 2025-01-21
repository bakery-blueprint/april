package com.demoversion.ch08

import java.util.concurrent.atomic.AtomicLong
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class Ch08Application

fun main(args: Array<String>) {
	runApplication<Ch08Application>(*args)
}


@RestController
@RequestMapping("/api/v1")
class UrlRedirectController(private val urlRedirectService: UrlRedirectService) {

    @PostMapping("/data/shorten")
    fun shortenUrl(@RequestBody request: Map<String, String>): ResponseEntity<String> {
        val longUrl = request["longUrl"] ?: return ResponseEntity.badRequest().body("Missing longUrl")
        val shortUrl = urlRedirectService.shorten(longUrl)
        return ResponseEntity.ok(shortUrl)
    }

    @GetMapping("/{shortUrl}")
    fun getLongUrl(@PathVariable shortUrl: String): ResponseEntity<String> {
        val longUrl = urlRedirectService.redirect(shortUrl)
        return if (longUrl.startsWith("Error")) {
            ResponseEntity.badRequest().body(longUrl)
        } else {
            ResponseEntity.ok(longUrl)
        }
    }
}

@Service
class UrlRedirectService {

    private val cache: MutableMap<String, String> = mutableMapOf()
    private val database: MutableMap<String, String> = mutableMapOf()
    private val idGenerator = AtomicLong(56800235584) // Start with a large number to ensure 7-character Base62

    init {
        initializeDatabase()
    }

    fun initializeDatabase() {
        database["zn9edcu"] = "https://en.wikipedia.org/wiki/Systems_design"
        database["abc123"] = "https://example.com/some-page"
    }

    fun shorten(longURL: String): String {
        if (database.containsValue(longURL)) {
            // Return existing short URL if the long URL already exists
            return database.entries.first { it.value == longURL }.key
        }

        // Generate a unique short URL
        val id = idGenerator.getAndIncrement()
        val shortUrl = id.toBase62()
        
        // Save to database and cache
        database[shortUrl] = longURL
        cache[shortUrl] = longURL
        return shortUrl
    }

    fun redirect(shortURL: String): String {
        if (cache.containsKey(shortURL)) {
            println("Cache hit for $shortURL")
            return cache[shortURL]!!
        }

        if (database.containsKey(shortURL)) {
            println("Cache miss for $shortURL. Fetching from database.")
            val longURL = database[shortURL]!!
            cache[shortURL] = longURL
            return longURL
        }

        println("Invalid short URL: $shortURL")
        return "Error: Invalid short URL"
    }

    private fun Long.toBase62(): String {
        val base62Chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var value = this
        val result = StringBuilder()
        while (value > 0) {
            result.append(base62Chars[(value % 62).toInt()])
            value /= 62
        }
        return result.reverse().toString()
    }
}
