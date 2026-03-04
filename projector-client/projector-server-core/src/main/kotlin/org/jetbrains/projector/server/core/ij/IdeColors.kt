
package org.jetbrains.projector.server.core.ij

import com.intellij.application.subscribe
import com.intellij.ide.ui.LafManagerListener
import com.intellij.util.ui.JBUI.CurrentTheme.Label.foreground
import com.intellij.util.ui.JBUI.CurrentTheme.Popup.headerBackground
import com.intellij.util.ui.JBUI.CurrentTheme.Popup.borderColor
import org.jetbrains.projector.common.protocol.data.PaintValue
import org.jetbrains.projector.common.protocol.toClient.ServerWindowColorsEvent
import org.jetbrains.projector.util.loading.UseProjectorLoader
import org.jetbrains.projector.util.loading.state.IdeState
import org.jetbrains.projector.util.loading.state.whenOccurred
import org.jetbrains.projector.util.logging.Logger

/**
 * Class retrieves color info from IDE settings.
 * Subscribe on LafManager and update colors on LAF change.
 * Calls provided in constructor onColorsChanged action on LAF change.
 */
@UseProjectorLoader
public class IdeColors(private val onColorsChanged: (ServerWindowColorsEvent.ColorsStorage) -> Unit) {

  private val logger = Logger<IdeColors>()

  public var colors: ServerWindowColorsEvent.ColorsStorage? = null
    private set

  init {
    IdeState.CONFIGURATION_STORE_INITIALIZED.whenOccurred("Getting IDE colors") {
      subscribeToIdeLafManager()
    }
  }

  private fun subscribeToIdeLafManager() {
    try {
      LafManagerListener.TOPIC.subscribe(null, LafManagerListener {
        readColors()?.let {
          colors = it
          onColorsChanged(it)
        }
      })
    }
    catch (e: Exception) {
      logger.error(e) { "Failed to subscribe to IDE LAF manager." }
    }
  }

  private fun readColors(): ServerWindowColorsEvent.ColorsStorage? {
    try {
      val windowHeaderActiveBackground = PaintValue.Color(headerBackground(true).rgb)
      val windowHeaderInactiveBackground = PaintValue.Color(headerBackground(false).rgb)

      val windowActiveBorder = PaintValue.Color(borderColor(true).rgb)
      val windowInactiveBorder = PaintValue.Color(borderColor(false).rgb)

      val windowHeaderActiveText = PaintValue.Color(foreground().rgb)
      val windowHeaderInactiveText = windowHeaderActiveText

      return ServerWindowColorsEvent.ColorsStorage(
        windowHeaderActiveBackground = windowHeaderActiveBackground,
        windowHeaderInactiveBackground = windowHeaderInactiveBackground,
        windowActiveBorder = windowActiveBorder,
        windowInactiveBorder = windowInactiveBorder,
        windowHeaderActiveText = windowHeaderActiveText,
        windowHeaderInactiveText = windowHeaderInactiveText
      )
    }
    catch (e: Exception) {
      logger.error(e) { "Failed to get IDE color scheme." }
      return null
    }
  }
}
