package com.mybrowser.core.bridge

/**
 * Pure Kotlin Toolbar Customization Engine & Validator.
 * Manages Safari-style Bottom Floating Bar slots (`leftSlots`, `rightSlots`),
 * slot reordering (`moveSlot`), alignment, auto-minimize, and gesture flags.
 *
 * INVARIANT: The far-right `SETTINGS` slot is permanently anchored by the shell.
 */
// [RUST-MIGRATION-TARGET]
// pub struct RustToolbarConfigEngine {
//     state: RwLock<ToolbarConfigRecord>,
// }
class ToolbarConfigImpl : ToolbarConfigContract {

  private data class ActionSpec(
    val id: String,
    val label: String,
    val defaultSide: String,
  )

  private val catalog =
    listOf(
      ActionSpec("BACK", "뒤로", "LEFT"),
      ActionSpec("FORWARD", "앞으로", "LEFT"),
      ActionSpec("REFRESH", "새로고침", "LEFT"),
      ActionSpec("BOOKMARK", "북마크", "LEFT"),
      ActionSpec("HISTORY", "방문 기록", "LEFT"),
      ActionSpec("DESKTOP_MODE", "PC 버전", "LEFT"),
      ActionSpec("SHARE", "공유", "LEFT"),
      ActionSpec("READER_MODE", "읽기 모드", "LEFT"),
      ActionSpec("HOME", "홈", "LEFT"),
      ActionSpec("TABS", "탭", "RIGHT"),
      ActionSpec("NEW_TAB", "새 탭", "RIGHT"),
      ActionSpec("SPLIT_VIEW", "분할", "RIGHT"),
      ActionSpec("FIND_IN_PAGE", "찾기", "RIGHT"),
      ActionSpec("QR_SCAN", "QR 스캔", "RIGHT"),
      ActionSpec("SCREENSHOT", "스크린샷", "RIGHT"),
      ActionSpec("LIBRARY", "보관함", "RIGHT"),
    )

  private val validActionIds = catalog.map { it.id }.toSet()
  private val validAlignments = setOf("CENTER", "LEFT_OFFSET", "RIGHT_OFFSET")

  private val lock = Any()
  private var leftSlots: MutableList<String> = mutableListOf("BACK", "FORWARD", "BOOKMARK")
  private var rightSlots: MutableList<String> = mutableListOf("TABS", "SPLIT_VIEW")
  private var alignment: String = "CENTER"
  private var autoMinimize: Boolean = true
  private var swipeBarSwitchTabs: Boolean = true
  private var pullDownRefresh: Boolean = true

  override fun getAvailableActionsJson(): String {
    val items =
      catalog.joinToString(",") { spec ->
        """{"id":${JsonMini.quote(spec.id)},"label":${JsonMini.quote(spec.label)},"defaultSide":${JsonMini.quote(spec.defaultSide)}}"""
      }
    return "[$items]"
  }

  override fun getConfigJson(): String = synchronized(lock) { serializeLocked() }

  override fun updateSlots(leftSlotsCsv: String, rightSlotsCsv: String): String =
    synchronized(lock) {
      val cleanLeft = parseAndValidateSlots(leftSlotsCsv, maxCount = MAX_SLOTS_PER_SIDE)
      val cleanRight =
        parseAndValidateSlots(
          csv = rightSlotsCsv,
          maxCount = MAX_SLOTS_PER_SIDE,
          excluded = cleanLeft.toSet(),
        )

      leftSlots = cleanLeft.ifEmpty { listOf("BACK") }.toMutableList()
      rightSlots = cleanRight.ifEmpty { listOf("TABS") }.toMutableList()
      serializeLocked()
    }

  override fun moveSlot(side: String, fromIndex: Int, toIndex: Int): String = synchronized(lock) {
    val target = if (side.equals("RIGHT", ignoreCase = true)) rightSlots else leftSlots
    if (fromIndex in target.indices && toIndex in target.indices && fromIndex != toIndex) {
      val item = target.removeAt(fromIndex)
      target.add(toIndex, item)
    }
    serializeLocked()
  }

  override fun setAlignment(alignment: String): String = synchronized(lock) {
    val upper = alignment.trim().uppercase()
    if (upper in validAlignments) {
      this.alignment = upper
    }
    serializeLocked()
  }

  override fun setAutoMinimize(enabled: Boolean): String = synchronized(lock) {
    this.autoMinimize = enabled
    serializeLocked()
  }

  override fun setGestures(swipeTabs: Boolean, pullRefresh: Boolean): String = synchronized(lock) {
    this.swipeBarSwitchTabs = swipeTabs
    this.pullDownRefresh = pullRefresh
    serializeLocked()
  }

  override fun restoreConfigJson(rawJson: String): String = synchronized(lock) {
    if (rawJson.isBlank()) return@synchronized serializeLocked()

    val leftArr = extractStringArray(rawJson, "leftSlots")
    val rightArr = extractStringArray(rawJson, "rightSlots")
    val align = extractString(rawJson, "alignment")
    val autoMin = extractBoolean(rawJson, "autoMinimize")
    val swipeTabs = extractBoolean(rawJson, "swipeBarSwitchTabs")
    val pullRefresh = extractBoolean(rawJson, "pullDownRefresh")

    if (leftArr.isNotEmpty() || rightArr.isNotEmpty()) {
      val cleanLeft = leftArr.filter { it in validActionIds }.distinct().take(MAX_SLOTS_PER_SIDE)
      val cleanRight =
        rightArr
          .filter { it in validActionIds && it !in cleanLeft }
          .distinct()
          .take(MAX_SLOTS_PER_SIDE)
      if (cleanLeft.isNotEmpty()) leftSlots = cleanLeft.toMutableList()
      if (cleanRight.isNotEmpty()) rightSlots = cleanRight.toMutableList()
    }
    if (align != null && align in validAlignments) alignment = align
    if (autoMin != null) autoMinimize = autoMin
    if (swipeTabs != null) swipeBarSwitchTabs = swipeTabs
    if (pullRefresh != null) pullDownRefresh = pullRefresh
    serializeLocked()
  }

  override fun resetDefaults(): String = synchronized(lock) {
    leftSlots = mutableListOf("BACK", "FORWARD", "BOOKMARK")
    rightSlots = mutableListOf("TABS", "SPLIT_VIEW")
    alignment = "CENTER"
    autoMinimize = true
    swipeBarSwitchTabs = true
    pullDownRefresh = true
    serializeLocked()
  }

  private fun parseAndValidateSlots(
    csv: String,
    maxCount: Int,
    excluded: Set<String> = emptySet(),
  ): List<String> {
    return csv
      .split(',')
      .map { it.trim().uppercase() }
      .filter { it in validActionIds && it !in excluded }
      .distinct()
      .take(maxCount)
  }

  private fun serializeLocked(): String {
    val leftJson = leftSlots.joinToString(",") { JsonMini.quote(it) }
    val rightJson = rightSlots.joinToString(",") { JsonMini.quote(it) }
    return """{"leftSlots":[$leftJson],"rightSlots":[$rightJson],"alignment":${JsonMini.quote(alignment)},"autoMinimize":$autoMinimize,"swipeBarSwitchTabs":$swipeBarSwitchTabs,"pullDownRefresh":$pullDownRefresh,"fixedTrailing":"SETTINGS"}"""
  }

  private fun extractStringArray(json: String, key: String): List<String> {
    val regex = Regex(""""$key"\s*:\s*\[([^\]]*)\]""")
    val body = regex.find(json)?.groupValues?.get(1) ?: return emptyList()
    return Regex(""""([^"]+)"""").findAll(body).map { it.groupValues[1] }.toList()
  }

  private fun extractString(json: String, key: String): String? {
    val regex = Regex(""""$key"\s*:\s*"([^"]+)"""")
    return regex.find(json)?.groupValues?.get(1)
  }

  private fun extractBoolean(json: String, key: String): Boolean? {
    val regex = Regex(""""$key"\s*:\s*(true|false)""")
    return regex.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
  }

  companion object {
    const val MAX_SLOTS_PER_SIDE = 3
  }
}
