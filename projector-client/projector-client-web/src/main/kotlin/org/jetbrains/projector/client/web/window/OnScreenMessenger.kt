package org.jetbrains.projector.client.web.window

import kotlinx.browser.window
import kotlinx.browser.document
import org.jetbrains.projector.client.common.canvas.Extensions.argbIntToRgbaString
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.web.misc.toDisplayType
import org.jetbrains.projector.client.web.state.LafListener
import org.jetbrains.projector.client.web.state.ProjectorUI
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.util.logging.Logger
import org.w3c.dom.*

object OnScreenMessenger : LafListener {

  private val logger = Logger<OnScreenMessenger>()

  private val header = WindowHeader().apply {
    undecorated = true
    visible = false
  }

  private val div = (document.createElement("div") as HTMLDivElement).apply {
    style.apply {
      position = "fixed"
      zIndex = "567"

      // put to center:
      width = "400px"
      top = "50%"
      left = "50%"
      transform = "translate(-50%, -50%)"

      padding = "5px"
    }
  }

  private val text = (document.createElement("p") as HTMLParagraphElement).apply {
    div.appendChild(this)
  }

  private val reload = (document.createElement("div") as HTMLDivElement).apply {
    //innerHTML = "<p>If you wish, you can try to <a onclick='location.reload();' href=''>reconnect</a>.</p>"
    //innerHTML = "<p>如果您愿意，您可以尝试 <a onclick='window.reconnectProjector();' href='javascript:void(0)'>重新连接</a>.</p>"
    val p = document.createElement("p") as HTMLParagraphElement
    p.textContent = "如果您愿意，您可以尝试 "

    val a = document.createElement("a") as HTMLAnchorElement
    a.textContent = "重新连接"
    a.href = "javascript:void(0)"
    
    a.onclick = { event ->
      event.preventDefault()
      p.innerHTML = "<span style=\"display:block;text-align:center;color:#0000ff;\">正在重新连接中...</span>"

      val dyncWindow = window.asDynamic()
      if (dyncWindow.reconnectProjector != undefined) {
        dyncWindow.reconnectProjector()
      } else {
        window.location.reload()
      }
    }

    p.appendChild(a)
    appendChild(p)

    div.appendChild(this)
  }

  init {
    lookAndFeelChanged()
  }

  fun showText(title: String, content: String, canReload: Boolean) {
    logger.info { "$title - $content" }

    header.title = title
    text.innerText = content

    reload.style.display = canReload.toDisplayType()

    if (div.parentElement == null) {
      document.body!!.appendChild(div)
    }

    updatePosition()
    header.visible = true
    header.zIndex = div.style.zIndex.toInt()
    header.draw()
  }

  fun updatePosition() {
    val userScalingRatio = ParamsProvider.USER_SCALING_RATIO
    val mainDivBounds = div.getBoundingClientRect()
    header.bounds = CommonRectangle(
      mainDivBounds.x * userScalingRatio,
      (mainDivBounds.y - ProjectorUI.headerHeight) * userScalingRatio,
      div.clientWidth * userScalingRatio,
      ProjectorUI.headerHeight * userScalingRatio
    )
  }

  fun hide() {
    if (div.parentElement != null) {
      div.remove()
    }
    header.visible = false
  }

  override fun lookAndFeelChanged() {
    header.lookAndFeelChanged()

    div.style.apply {
      backgroundColor = ProjectorUI.windowHeaderInactiveBackgroundArgb.argbIntToRgbaString()
      borderLeft = ProjectorUI.borderStyle
      borderRight = ProjectorUI.borderStyle
      borderBottom = ProjectorUI.borderStyle
      borderRadius = "0 0 ${ProjectorUI.borderRadius}px ${ProjectorUI.borderRadius}px"
      color = ProjectorUI.windowHeaderActiveTextArgb.argbIntToRgbaString()
    }
  }
}
