package dev.zacsweers

import com.ankushg.Config
import com.ankushg.atom.AtomBlogApi
import com.ankushg.atom.InstantParseTypeConverter
import com.ankushg.displayText
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
  val activity = runBlocking {
    githubApi.getUserActivity(
      username = "ankushg",
      perPage = 100
    )
  }

  return activity
    .asSequence()
    .filter {event ->
      event.public
          && event.displayText != null
          && event.displayText !in Config.blocklistedActivityStrings
    }
    .map { event ->
      ActivityItem(event.displayText!!, event.createdAt)
    }.take(10)
    .toList()
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