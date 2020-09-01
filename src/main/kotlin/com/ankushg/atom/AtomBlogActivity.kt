package com.ankushg.atom

import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.TypeConverter
import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.tickaroo.tikxml.converter.htmlescape.HtmlEscapeStringConverter
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import java.time.Instant

// https://ankushg.com/feed

internal interface AtomBlogApi {
    @GET("/feed")
    suspend fun main(): Feed

    companion object {
        fun create(client: OkHttpClient, tikXml: TikXml): AtomBlogApi {
            return Retrofit.Builder()
                    .baseUrl("https://ankushg.com")
                    .validateEagerly(true)
                    .client(client)
                    .addConverterFactory(TikXmlConverterFactory.create(tikXml))
                    .build()
                    .create()
        }
    }
}

@Xml
data class Feed(
        @PropertyElement
        val id: String,

        @PropertyElement
        val title: String,

        @PropertyElement(converter = InstantParseTypeConverter::class)
        val updated: Instant,

        @Element
        val entries: List<Entry>,

        // Optional elements
        @PropertyElement
        val subtitle: String? = null
)

@Xml(name = "entry")
data class Entry(
        @PropertyElement
        val id: String,
        @PropertyElement(converter = HtmlEscapeStringConverter::class)
        val title: String,
        @PropertyElement(converter = InstantParseTypeConverter::class)
        val updated: Instant,

        // Optional elements
        @Element
        val link: Link? = null,

        @PropertyElement(converter = HtmlEscapeStringConverter::class)
        val summary: String? = null,

        @PropertyElement(converter = InstantParseTypeConverter::class)
        val published: Instant? = null,
)

@Xml
data class Link(
        @Attribute(name = "href")
        val uri: String? = null,

        @Attribute(name = "title", converter = HtmlEscapeStringConverter::class)
        val title: String? = null
)

/**
 * Atom requires RFC 3339-formatted dates, which is compatible with [Instant.parse]
 */
internal class InstantParseTypeConverter : TypeConverter<Instant> {
    override fun write(value: Instant): String = TODO("Unsupported")

    override fun read(value: String): Instant {
        return Instant.parse(value)
    }
}