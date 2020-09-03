package dev.zacsweers

import com.ankushg.Config
import com.ankushg.atom.AtomBlogApi
import com.ankushg.atom.InstantParseTypeConverter
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.squareup.moshi.Moshi
import com.tickaroo.tikxml.TikXml
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.time.Instant
import java.time.ZoneId
import kotlin.system.exitProcess

class UpdateReadmeCommand : CliktCommand() {

  val outputFile by option("-o", help = "The README.md file to write")
    .file()
    .required()

  override fun run() {
    val okHttpClient = OkHttpClient.Builder()
      .build()

    val githubActivity = fetchGithubActivity(okHttpClient)
    val blogActivity = fetchBlogActivity(okHttpClient)

    val newReadMe = createReadMe(githubActivity, blogActivity)
    outputFile.writeText(newReadMe)

    // TODO why do I need to do this
    exitProcess(0)
  }
}

private fun fetchBlogActivity(
  client: OkHttpClient
): List<ActivityItem> {
  val blogApi = AtomBlogApi.create(
    client, TikXml.Builder()
      .exceptionOnUnreadXml(false)
      .addTypeConverter(Instant::class.java, InstantParseTypeConverter())
      .build()
  )

  return runBlocking { blogApi.main().entries }
    .map { entry ->
      val text = entry.link?.uri?.let { uri -> "[${entry.title}](${uri})" } ?: entry.title
      ActivityItem(
        text = text,
        timestamp = entry.published ?: entry.updated
      )
    }
    .take(10)
}

private fun fetchGithubActivity(
  client: OkHttpClient
): List<ActivityItem> {
  val moshi = Moshi.Builder().build()
  val githubApi = GitHubApi.create(client, moshi)
  val activity = runBlocking { githubApi.getUserActivity("ankushg") }

  return activity
    .filter { it.public }
    .mapNotNull { event ->
      when (val payload = event.payload) {
        UnknownPayload, null -> return@mapNotNull null
        is IssuesEventPayload -> {
          ActivityItem(
            "${payload.action} issue [#${payload.issue.number}](${payload.issue.url}) on ${event.repo?.markdownUrl()}: \"${payload.issue.title}\"",
            event.createdAt
          )
        }
        is IssueCommentEventPayload -> {
          ActivityItem(
            "commented on [#${payload.issue.number}](${payload.comment.htmlUrl}) in ${event.repo?.markdownUrl()}",
            event.createdAt
          )
        }
        is PushEventPayload -> {
          ActivityItem(
            payload.commitMessage(event),
            event.createdAt
          )
        }
        is PullRequestPayload -> {
          ActivityItem(
            "${payload.action} PR [#${payload.number}](${payload.pullRequest.url}) to ${event.repo?.markdownUrl()}: \"${payload.pullRequest.title}\"",
            event.createdAt
          )
        }
        is CreateEvent -> {
          ActivityItem(
            "created ${payload.refType}${payload.ref?.let { " \"$it\"" } ?: ""} on ${event.repo?.markdownUrl()}",
            event.createdAt
          )
        }
        is DeleteEvent -> {
          ActivityItem(
            "deleted ${payload.refType}${payload.ref?.let { " \"$it\"" } ?: ""} on ${event.repo?.markdownUrl()}",
            event.createdAt
          )
        }
      }
    }.filter {item ->
        !Config.blocklistedActivityStrings.any { it in item.text }
    }
    .take(10)
}

fun main(argv: Array<String>) {
  UpdateReadmeCommand().main(argv)
}

data class ActivityItem(
  val text: String,
  val timestamp: Instant
) {
  override fun toString(): String {
    return "**${timestamp.atZone(ZoneId.of("America/Los_Angeles")).toLocalDate()}** — $text"
  }
}