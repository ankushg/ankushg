package dev.zacsweers

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.lang.IllegalStateException
import java.lang.reflect.Type
import java.time.Instant
import kotlin.reflect.KClass

interface GitHubApi {
  @GET("/users/{username}/events/public")
  suspend fun getUserActivity(
    @Path("username") username: String,
    @Query("per_page") perPage: Int = 30
  ): List<GitHubActivityEvent>

  companion object {
    fun create(
      client: OkHttpClient,
      moshi: Moshi
    ): GitHubApi {
      return Retrofit.Builder()
        .baseUrl("https://api.github.com")
        .validateEagerly(true)
        .client(client)
        .addConverterFactory(
          MoshiConverterFactory.create(
            moshi.newBuilder()
              .add(
                DefaultOnDataMismatchAdapter.newFactory(
                  GitHubActivityEventPayload.Type::class.java,
                  GitHubActivityEventPayload.Type.UNKNOWN
                )
              )
              .add(GitHubActivityEvent.Factory)
              .build()
          )
        )
        .build()
        .create()
    }
  }
}

data class GitHubActivityEvent(
  val id: String,
  val createdAt: Instant,
  val payload: GitHubActivityEventPayload?,
  val public: Boolean,
  val repo: Repo?
) {
  companion object Factory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
      if (Types.getRawType(type) != GitHubActivityEvent::class.java) return null
      if (annotations.isNotEmpty()) return null

      val typeAdapter = moshi.adapter(GitHubActivityEventPayload.Type::class.java)
      val repoAdapter = moshi.adapter(Repo::class.java)
      return object : JsonAdapter<GitHubActivityEvent>() {
        override fun fromJson(reader: JsonReader): GitHubActivityEvent {
          @Suppress("UNCHECKED_CAST")
          val value = reader.readJsonValue() as Map<String, *>
          val payloadType = value["type"]?.toString()?.let(typeAdapter::fromJsonValue) ?: error("No type found")
          val payloadValue = value["payload"]
          val payload = if (payloadType != GitHubActivityEventPayload.Type.UNKNOWN && payloadValue != null) {
            moshi.adapter(payloadType.subclass.java)
              .fromJsonValue(payloadValue)
          } else {
            null
          }
          val id = value["id"]?.toString() ?: error("No id found")
          val createdAt = value["created_at"]?.toString() ?: error("No created_at found")
          val public = value["public"]?.toString()?.toBoolean() ?: error("No public found")
          val repo = value["repo"]?.let { repoAdapter.fromJsonValue(it) }

          val createdAtInstant = Instant.parse(createdAt)
          return GitHubActivityEvent(id, createdAtInstant, payload, public, repo)
        }

        override fun toJson(writer: JsonWriter, value: GitHubActivityEvent?) {
          throw NotImplementedError()
        }
      }
    }
  }
}


sealed interface GitHubActivityEventPayload {
  enum class Type(val subclass: KClass<out GitHubActivityEventPayload>) {
    UNKNOWN(UnknownPayload::class),

    @Json(name = "IssuesEvent")
    ISSUE(IssuesEventPayload::class),

    @Json(name = "IssueCommentEvent")
    ISSUE_COMMENT(IssueCommentEventPayload::class),

    @Json(name = "PushEvent")
    PUSH(PushEventPayload::class),

    @Json(name = "PullRequestEvent")
    PULL_REQUEST(PullRequestPayload::class),

    @Json(name = "CreateEvent")
    CREATE_EVENT(CreateEvent::class),

    @Json(name = "DeleteEvent")
    DELETE_EVENT(DeleteEvent::class)
  }
}

object UnknownPayload : GitHubActivityEventPayload

@JsonClass(generateAdapter = true)
data class IssuesEventPayload(
  val action: String,
  val issue: Issue
) : GitHubActivityEventPayload

@JsonClass(generateAdapter = true)
data class Issue(
  val title: String,
  val body: String? = null,
  val url: String,
  val number: Int,
  @Json(name = "html_url")
  val htmlUrl: String? = null
) {
  val displayUrl = htmlUrl ?: url
}

@JsonClass(generateAdapter = true)
data class IssueCommentEventPayload(
  val action: String,
  val comment: Comment,
  val issue: Issue
) : GitHubActivityEventPayload

@JsonClass(generateAdapter = true)
data class Comment(
  @Json(name = "html_url")
  val htmlUrl: String? = null,
  val body: String,
  val url: String
) {
  val displayUrl
    get() = htmlUrl ?: url
}

@JsonClass(generateAdapter = true)
data class PushEventPayload(
  val head: String,
  val size: Int,
  @Json(name = "distinct_size")
  val distinctSize: Int,
  val commits: List<Commit>
) : GitHubActivityEventPayload

@JsonClass(generateAdapter = true)
data class Commit(
  val sha: String,
  val message: String,
  val url: String,
  @Json(name = "html_url")
  val htmlUrl: String? = null
) {
  fun title(): String = message
      .substringBefore("\n")

  val displayUrl: String
    get() = htmlUrl ?: adjustedApiUrl()

  private fun adjustedApiUrl(): String = url
          .replace("api.", "")
          .replace("/repos/", "/")
          .replace("/commits/", "/commit/")
}

@JsonClass(generateAdapter = true)
data class PullRequestPayload(
  val action: String,
  val number: Int,
  @Json(name = "pull_request")
  val pullRequest: PullRequest
) : GitHubActivityEventPayload

@JsonClass(generateAdapter = true)
data class PullRequest(
  val url: String,
  @Json(name = "html_url")
  val htmlUrl: String? = null,
  val title: String,
  val body: String? = null
) {
  val displayUrl: String
    get() = htmlUrl ?: url
}

@JsonClass(generateAdapter = true)
data class Repo(
  val name: String,
  val url: String,
  @Json(name = "html_url")
  val htmlUrl: String? = null
) {
  val displayUrl: String
    get() = htmlUrl ?: url
}

@JsonClass(generateAdapter = true)
data class CreateEvent(
  val ref: String?,
  @Json(name = "ref_type")
  val refType: String
) : GitHubActivityEventPayload

@JsonClass(generateAdapter = true)
data class DeleteEvent(
  val ref: String?,
  @Json(name = "ref_type")
  val refType: String
) : GitHubActivityEventPayload