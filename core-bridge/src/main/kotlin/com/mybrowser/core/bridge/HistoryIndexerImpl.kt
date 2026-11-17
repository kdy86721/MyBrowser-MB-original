package com.mybrowser.core.bridge

import kotlin.math.exp
import kotlin.math.ln

/**
 * Pure Kotlin Frecency & Prefix/Token History/Bookmark Indexer.
 * Accepts flat JSON candidate lists and returns ranked JSON omnibox suggestions.
 */
// [RUST-MIGRATION-TARGET]
// #[uniffi::export]
// pub fn score_frecency(visit_count: i32, last_visited_epoch_ms: i64, now_epoch_ms: i64) -> f64
// // Replaceable with Rust `nucleo` / `tantivy` prefix-fuzzy matcher.
class HistoryIndexerImpl : HistoryIndexerContract {

  override fun scoreFrecency(
    visitCount: Int,
    lastVisitedEpochMs: Long,
    nowEpochMs: Long,
  ): Double {
    val ageHours = ((nowEpochMs - lastVisitedEpochMs).coerceAtLeast(0L)) / 3_600_000.0
    val recencyDecay = exp(-0.015 * ageHours)
    val frequencyBoost = 1.0 + ln(1.0 + visitCount.coerceAtLeast(1).toDouble())
    return recencyDecay * frequencyBoost * 100.0
  }

  override fun rankSuggestionsJson(
    query: String,
    candidatesJson: String,
    nowEpochMs: Long,
  ): String {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return "[]"

    // Parse flat JSON array of {"url":"...","title":"...","visitCount":N,"lastVisited":L,"isBookmark":B}
    val parsed = parseFlatCandidates(candidatesJson)
    val ranked =
      parsed
        .mapNotNull { item ->
          val urlLower = item.url.lowercase()
          val titleLower = item.title.lowercase()
          val matchWeight =
            when {
              urlLower.contains("://$q") || urlLower.startsWith(q) -> 3.0
              titleLower.startsWith(q) -> 2.6
              urlLower.contains(q) -> 2.0
              titleLower.contains(q) -> 1.6
              else -> return@mapNotNull null
            }
          val baseFrecency = scoreFrecency(item.visitCount, item.lastVisited, nowEpochMs)
          val bookmarkBonus = if (item.isBookmark) 35.0 else 0.0
          val totalScore = baseFrecency * matchWeight + bookmarkBonus
          item to totalScore
        }
        .sortedByDescending { it.second }
        .take(6)

    val out =
      ranked.joinToString(",") { (item, score) ->
        """{"url":${JsonMini.quote(item.url)},"title":${JsonMini.quote(item.title)},"isBookmark":${item.isBookmark},"score":$score}"""
      }
    return "[$out]"
  }

  private data class CandidateItem(
    val url: String,
    val title: String,
    val visitCount: Int,
    val lastVisited: Long,
    val isBookmark: Boolean,
  )

  private fun parseFlatCandidates(json: String): List<CandidateItem> {
    val objects = objectRegex.findAll(json).map { it.value }.toList()
    return objects.mapNotNull { obj ->
      val url = extractStringField(obj, "url") ?: return@mapNotNull null
      val title = extractStringField(obj, "title") ?: url
      val visitCount = extractNumberField(obj, "visitCount")?.toInt() ?: 1
      val lastVisited = extractNumberField(obj, "lastVisited") ?: 0L
      val isBookmark = extractBooleanField(obj, "isBookmark") ?: false
      CandidateItem(url, title, visitCount, lastVisited, isBookmark)
    }
  }

  private fun extractStringField(obj: String, key: String): String? {
    val regex = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
    val match = regex.find(obj) ?: return null
    return match.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
  }

  private fun extractNumberField(obj: String, key: String): Long? {
    val regex = Regex(""""$key"\s*:\s*(-?\d+)""")
    return regex.find(obj)?.groupValues?.get(1)?.toLongOrNull()
  }

  private fun extractBooleanField(obj: String, key: String): Boolean? {
    val regex = Regex(""""$key"\s*:\s*(true|false)""")
    return regex.find(obj)?.groupValues?.get(1)?.toBooleanStrictOrNull()
  }

  private companion object {
    val objectRegex = Regex("""\{[^{}]*\}""")
  }
}
