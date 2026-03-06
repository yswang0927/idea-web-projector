package org.jetbrains.projector.client.web.window

import kotlinx.browser.document
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.common.protocol.data.ImageId
import org.jetbrains.projector.common.protocol.toClient.ServerWindowSetChangedEvent
import org.jetbrains.projector.common.protocol.toClient.WindowData
import org.jetbrains.projector.common.protocol.toClient.WindowType
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLLinkElement
import kotlin.collections.isNotEmpty

class WindowDataEventsProcessor(private val windowManager: WebWindowManager) {

  var excludedWindowIds = emptyList<Int>()
    private set


  // yswang add：用来保证 onWindowReady 只执行一次
  private var isWindowReadyFired = false

  fun onClose() {
    process(ServerWindowSetChangedEvent(emptyList()))
  }

  fun process(windowDataEvents: ServerWindowSetChangedEvent) {
    val excludedWindows = when (val selectedId = ParamsProvider.IDE_WINDOW_ID) {
      null -> emptyList()

      else -> windowDataEvents.windowDataList
        .filter { it.windowType == WindowType.IDEA_WINDOW }
        .sortedBy(WindowData::id)
        .filterIndexed { index, _ -> index != selectedId }
    }
    excludedWindowIds = excludedWindows.map(WindowData::id)  // todo: try to use ClientWindowInterestEvent instead of filtering on client
    val presentedWindows = windowDataEvents.windowDataList.subtract(excludedWindows)

    removeAbsentWindows(presentedWindows)

    presentedWindows.forEach { event ->
      val window = windowManager.getOrCreate(event)
      event.cursorType?.let { window.cursorType = it }
      window.title = event.title
      window.isShowing = event.isShowing
      window.bounds = event.bounds
      window.zIndex = (event.zOrder - presentedWindows.size) * WebWindowManager.zIndexStride
    }

    setTitle(presentedWindows)
    setFavIcon(presentedWindows)

    // yswang add: 判断是否是主窗口
    if (!isWindowReadyFired) {
      val isIdeReady = presentedWindows
        .filter { it.isShowing }
        .any { win ->
          val winClass = win.windowClass?.toString() ?: ""
          val isModal = win.modal ?: false

          // 1：如果是真正的项目窗口，绝对放行 (应对 IDEA 自动恢复了上次的项目)
          if (win.windowType == WindowType.IDEA_WINDOW && !isModal) {
            return@any true
          }

          // 2：如果是普通窗口，需要判断是不是 Welcome 界面
          if (!"FRAME".equals(winClass, ignoreCase = true) || isModal) {
            return@any false
          }

          if (win.windowType == WindowType.WINDOW && win.bounds.width > 200 && win.bounds.height > 100) {
            val title = win.title ?: ""
            val isEulaOrDialog = title.contains("Agreement", ignoreCase = true)
                                  || title.contains("License", ignoreCase = true)
                                  || title.contains("用户协议")
                                  || title.contains("Data Sharing", ignoreCase = true)
                                  || title.contains("数据共享")
            
            // win.title == null 表示 Splash Screen (启动画面)
            if (win.title != null && !isEulaOrDialog) {
              return@any true
            }
          }
          
          false // 其他小弹窗、EUA 弹窗一律无视
        }
      
      if (isIdeReady) {
        isWindowReadyFired = true // 立刻锁死，防止重复触发
        kotlinx.browser.window.dispatchEvent(org.w3c.dom.CustomEvent("projectorWindowReady"))
      }
    }

  }

  private fun setTitle(presentedWindows: Iterable<WindowData>) {
    val topmostWindowTitle = presentedWindows
      .filter(WindowData::isShowing)
      .sortedByDescending(WindowData::zOrder)
      .firstNotNullOfOrNull(WindowData::title)

    document.title = topmostWindowTitle ?: DEFAULT_TITLE
  }

  private fun setFavIcon(presentedWindows: Iterable<WindowData>) {
    val topmostWindowIconIds = presentedWindows
      .filter(WindowData::isShowing)
      .sortedByDescending(WindowData::zOrder)
      .mapNotNull(WindowData::icons)
      .firstOrNull(List<*>::isNotEmpty)

    fun selectIcon(icons: List<ImageId>?) = icons?.firstOrNull()  // todo

    val selectedIconId = selectIcon(topmostWindowIconIds)

    val selectedIconUrl = when (val selectedIcon = selectedIconId?.let { windowManager.imageCacher.getImageData(it) }) {
      is HTMLCanvasElement -> selectedIcon.toDataURL()
      is HTMLImageElement -> selectedIcon.src
      else -> "pj.svg"
    }

    fun getFavIconLink() = document.querySelector("link[rel*='icon']") ?: document.createElement("link")

    val link = (getFavIconLink() as HTMLLinkElement).apply {
      type = "image/x-icon"
      rel = "shortcut icon"
      href = selectedIconUrl
    }
    document.head!!.appendChild(link)
  }

  private fun removeAbsentWindows(presentedWindows: Iterable<WindowData>) {
    val presentedWindowIds = presentedWindows.map(WindowData::id).toSet()

    windowManager.cleanup(presentedWindowIds)
  }

  fun onResized() {
    windowManager.forEach(WebWindow::applyBounds)
  }

  companion object {
    private const val DEFAULT_TITLE = "Projector"
  }
}
