package org.jetbrains.projector.client.web.window

import kotlinx.browser.window
import org.jetbrains.projector.client.common.misc.ImageCacher
import org.jetbrains.projector.client.common.window.WindowManager
import org.jetbrains.projector.client.web.state.ClientAction
import org.jetbrains.projector.client.web.state.ClientStateMachine
import org.jetbrains.projector.client.web.state.LafListener
import org.jetbrains.projector.common.protocol.toClient.WindowData
import org.jetbrains.projector.common.protocol.toServer.ClientWindowsActivationEvent
import org.jetbrains.projector.common.protocol.toServer.ClientWindowsDeactivationEvent

import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.common.protocol.toServer.ClientOpenProjectEvent
import org.jetbrains.projector.common.protocol.toServer.ClientOpenFileEvent

import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.FocusEvent

class WebWindowManager(private val stateMachine: ClientStateMachine, override val imageCacher: ImageCacher) : WindowManager<WebWindow>, LafListener {

  companion object {
    const val zIndexStride = 10
  }

  init {
    window.onblur = ::onDeactivated
    window.onfocus = ::onActivated

    // --- yswang 新增逻辑 ---
    // --- 将方法暴露给全局 JS (window 对象) ---
    // 这样你在浏览器控制台输入 window.projectorOpenProject(...) 就能调用
    (window.asDynamic()).projectorOpenProject = ::openProject
    (window.asDynamic()).projectorOpenFile = ::openFile

    // 检查 URL 参数启动参数
    ParamsProvider.PROJECT_PATH?.let { openProject(it) }
    ParamsProvider.FILE_PATH?.let { openFile(it, ParamsProvider.FILE_LINE ?: 0) }
    // ----------------
  }

  private val visibleWindows get() = windows.values.filter { it.isShowing }

  // todo: remove SUPPRESS after KT-8112 is implemented or KTIJ-15401 is solved in some other way
  private fun onActivated(@Suppress("UNUSED_PARAMETER") event: FocusEvent) {
    val windowIds = visibleWindows.map { it.id }
    stateMachine.fire(ClientAction.AddEvent(ClientWindowsActivationEvent(windowIds)))
  }

  // todo: remove SUPPRESS after KT-8112 is implemented or KTIJ-15401 is solved in some other way
  private fun onDeactivated(@Suppress("UNUSED_PARAMETER") event: FocusEvent) {
    val windowIds = visibleWindows.map { it.id }
    stateMachine.fire(ClientAction.AddEvent(ClientWindowsDeactivationEvent(windowIds)))
  }

  private val windows = mutableMapOf<Int, WebWindow>()

  fun getWindowCanvas(windowId: Int): HTMLCanvasElement? = windows[windowId]?.canvas

  fun getWindowZIndex(windowId: Int): Int? = windows[windowId]?.zIndex

  /** Returns topmost visible window, containing point. Contain check includes window header and borders.  */
  fun getTopWindow(x: Int, y: Int): WebWindow? = windows.values.filter { it.isShowing && it.contains(x, y) }.maxByOrNull { it.zIndex }

  fun getOrCreate(windowData: WindowData): WebWindow {
    return windows.getOrPut(windowData.id) { WebWindow(windowData, stateMachine, imageCacher) }
  }

  fun cleanup(presentedWindowIds: Set<Int>) {
    windows.keys.retainAll { id ->
      if (id in presentedWindowIds) {
        true
      }
      else {
        windows.getValue(id).dispose()
        false
      }
    }
  }

  override fun lookAndFeelChanged() {
    windows.forEach { it.value.lookAndFeelChanged() }
  }

  override operator fun get(windowId: Int): WebWindow? = windows[windowId]

  override fun iterator(): Iterator<WebWindow> = windows.values.iterator()

  fun bringToFront(window: WebWindow) {
    val topWindow = windows.maxByOrNull { it.value.zIndex }?.value ?: return
    if (topWindow == window) {
      return
    }

    val currentZIndex = window.zIndex
    val topZIndex = topWindow.zIndex

    windows.filter { it.value.zIndex in currentZIndex..topZIndex }.forEach { it.value.zIndex -= zIndexStride }
    window.zIndex = topZIndex
  }

  // yswang add
  /**
   * 封装打开项目逻辑
   */
  fun openProject(path: String) {
    if (path == null || path.isBlank()) return
    stateMachine.fire(ClientAction.AddEvent(ClientOpenProjectEvent(projectPath = path)))
    console.log(">> Projector: Requesting to open project: $path")
  }

  /**
   * 供外部调用的打开文件方法
   * @param path 绝对路径
   * @param line 行号 (从1开始)
   */
  fun openFile(path: String, line: Int = 0) {
    if (path == null || path.isBlank()) return
    stateMachine.fire(ClientAction.AddEvent(ClientOpenFileEvent(
      filePath = path,
      line = line
    )))
    console.log(">> Projector: Requesting to open file: $path at line: $line")
  }
  
}
