package com.ankushg

import dev.zacsweers.*

val GitHubActivityEvent.displayText: String?
    get() = when (this.payload) {
        UnknownPayload, null -> null
        is IssuesEventPayload -> "${payload.action} issue ${payload.markdownLink} on ${repo?.markdownLink}: \"${payload.issue.title}\""
        is IssueCommentEventPayload -> "commented on ${payload.markdownLink} in ${repo?.markdownLink}"
        is PushEventPayload -> payload.commitMessage(this.repo)
        is PullRequestPayload -> "${payload.action} PR ${payload.markdownLink} to ${repo?.markdownLink}: \"${payload.pullRequest.title}\""
        is CreateEvent -> "created ${payload.refType}${payload.ref?.let { " \"$it\"" } ?: ""} on ${repo?.markdownLink}"
        is DeleteEvent -> "deleted ${payload.refType}${payload.ref?.let { " \"$it\"" } ?: ""} on ${repo?.markdownLink}"
    }

private val Repo.markdownLink: String
    get() = "[$name]($displayUrl)"

private val Commit.displaySha: String
    get() = sha.substring(0..7)

private val Commit.markdownLink: String
    get() = "[`${displaySha}`](${displayUrl})"

private val IssuesEventPayload.markdownLink: String
    get() = "[#${issue.number}](${issue.displayUrl})"

private val IssueCommentEventPayload.markdownLink: String
    get() = "[#${issue.number}](${comment.displayUrl})"

private val PullRequestPayload.markdownLink: String
    get() = "[#${number}](${pullRequest.displayUrl})"

private fun PushEventPayload.commitMessage(repo: Repo?): String {
    return if (distinctSize == 1) {
        val commit = commits.first()
        "pushed ${commit.markdownLink} to ${repo?.markdownLink}: \"${commit.title()}\""
    } else {
        "pushed $size commits to ${repo?.markdownLink}."
    }
}