package com.soundfusion.integration.podcast

import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

data class RssFeed(
    val title: String,
    val author: String,
    val imageUrl: String?,
    val episodes: List<PodcastEpisode>,
)

@Singleton
class RssParser @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun parse(feedUrl: String): List<PodcastEpisode> {
        val xml = fetchFeed(feedUrl) ?: return emptyList()
        return parseXml(xml)
    }

    suspend fun parseFull(feedUrl: String): RssFeed? {
        val xml = fetchFeed(feedUrl) ?: return null
        return parseFullFeed(xml)
    }

    private fun fetchFeed(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) response.body?.string() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseXml(xml: String): List<PodcastEpisode> {
        val episodes = mutableListOf<PodcastEpisode>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var inItem = false
        var currentTitle = ""
        var currentGuid = ""
        var currentAuthor = ""
        var currentDuration = ""
        var currentAudioUrl = ""
        var currentImageUrl: String? = null
        var currentTag = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        inItem = true
                        currentTitle = ""
                        currentGuid = ""
                        currentAuthor = ""
                        currentDuration = ""
                        currentAudioUrl = ""
                        currentImageUrl = null
                    }
                    if (inItem && currentTag == "enclosure") {
                        currentAudioUrl = parser.getAttributeValue(null, "url") ?: ""
                    }
                    if (inItem && currentTag == "itunes:image") {
                        currentImageUrl = parser.getAttributeValue(null, "href")
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inItem) {
                        when (currentTag) {
                            "title" -> currentTitle = parser.text?.trim() ?: ""
                            "guid" -> currentGuid = parser.text?.trim() ?: ""
                            "itunes:author" -> currentAuthor = parser.text?.trim() ?: ""
                            "itunes:duration" -> currentDuration = parser.text?.trim() ?: ""
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && inItem) {
                        inItem = false
                        if (currentAudioUrl.isNotBlank()) {
                            episodes.add(PodcastEpisode(
                                guid = currentGuid.ifBlank { currentAudioUrl.hashCode().toString() },
                                title = currentTitle,
                                author = currentAuthor,
                                durationMs = parseDuration(currentDuration),
                                imageUrl = currentImageUrl,
                                audioUrl = currentAudioUrl,
                            ))
                        }
                    }
                    currentTag = ""
                }
            }
            parser.next()
        }
        return episodes
    }

    private fun parseFullFeed(xml: String): RssFeed {
        val episodes = parseXml(xml)
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var feedTitle = ""
        var feedAuthor = ""
        var feedImage: String? = null
        var inChannel = false
        var inItem = false
        var currentTag = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "channel") inChannel = true
                    if (currentTag == "item") inItem = true
                    if (inChannel && !inItem && currentTag == "itunes:image") {
                        feedImage = parser.getAttributeValue(null, "href")
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inChannel && !inItem) {
                        when (currentTag) {
                            "title" -> if (feedTitle.isBlank()) feedTitle = parser.text?.trim() ?: ""
                            "itunes:author" -> if (feedAuthor.isBlank()) feedAuthor = parser.text?.trim() ?: ""
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") inItem = false
                    currentTag = ""
                }
            }
            parser.next()
        }

        return RssFeed(
            title = feedTitle,
            author = feedAuthor,
            imageUrl = feedImage,
            episodes = episodes,
        )
    }

    private fun parseDuration(raw: String): Long {
        if (raw.isBlank()) return 0L
        val parts = raw.split(":")
        return when (parts.size) {
            3 -> (parts[0].toLongOrNull() ?: 0) * 3600_000 + (parts[1].toLongOrNull() ?: 0) * 60_000 + (parts[2].toLongOrNull() ?: 0) * 1000
            2 -> (parts[0].toLongOrNull() ?: 0) * 60_000 + (parts[1].toLongOrNull() ?: 0) * 1000
            1 -> (parts[0].toLongOrNull() ?: 0) * 1000
            else -> 0L
        }
    }
}
