package dev.zacsweers

fun createReadMe(
  githubActivity: List<ActivityItem>,
  blogActivity: List<ActivityItem>
): String {
  return """
    Currently working at [Quizlet](https://quizlet.com/). Read [my blog](https://ankushg.com/) or [follow @ankushg on Twitter](https://twitter.com/ankushg).

    <table><tr><td valign="top" width="40%">

    ## On My Blog
    <!-- blog starts -->
${blogActivity.joinToString("\n\n") { "    $it" }}
    <!-- blog ends -->
    More on [ankushg.com](https://ankushg.com/)
    </td><td valign="top" width="60%">

    ## Public GitHub Activity
    <!-- githubActivity starts -->
${githubActivity.joinToString("\n\n") { "    $it" }}
    <!-- githubActivity ends -->
    </td></tr></table>
    
    <sub><a href="https://github.com/ZacSweers/ZacSweers">Based on Zac Sweer's auto-updating profile README</a>, which is <a href="https://simonwillison.net/2020/Jul/10/self-updating-profile-readme/">inspired by Simon Willison's auto-updating profile README.</a></sub>
  """.trimIndent()
}