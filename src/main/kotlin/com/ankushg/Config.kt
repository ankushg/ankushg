package com.ankushg

object Config {
    val blocklistedActivityStrings = listOf(
            "forestry.io", // disable forestry CMS updates from showing
            "ankushg/ankushg", // disable updates to personal blog or readme from showing
            "mmistakes/mm-github-pages-starter" // disable accidental PRs to blog upstream
    )
}