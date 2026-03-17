/*
 * MIT License
 *
 * Copyright (c) 2019-2023 JetBrains s.r.o.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.jetbrains.projector.server.core.util

import org.java_websocket.WebSocketServerFactory
import org.java_websocket.server.DefaultSSLWebSocketServerFactory
import org.jetbrains.projector.util.loading.getOption
import org.jetbrains.projector.util.logging.Logger
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

private val logger = Logger("KeyKt")

public const val SSL_ENV_NAME: String = "ORG_JETBRAINS_PROJECTOR_SERVER_SSL_PROPERTIES_PATH"
public const val SSL_STORE_TYPE: String = "STORE_TYPE"
public const val SSL_FILE_PATH: String = "FILE_PATH"
public const val SSL_STORE_PASSWORD: String = "STORE_PASSWORD"
public const val SSL_KEY_PASSWORD: String = "KEY_PASSWORD"

public data class SSLProperties(
  val storeType: String,
  val filePath: String,
  val storePassword: String,
  val keyPassword: String,
)

// yswang add 支持动态替换 properties 变量值中的占位符: ${xxx}
private fun resolvePlaceholders(str: String): String {
  val regex = Regex("\\$\\{([^}]+)}")
  return regex.replace(str) { matchResult ->
    val key = matchResult.groupValues[1]
    // 优先级查找：JVM 属性 -> 环境变量 -> 找不到则原样保留占位符
    System.getProperty(key) ?: System.getenv(key) ?: matchResult.value
  }
}

public fun loadSSLProperties(path: String): SSLProperties {
  // yswang modify
  fun Properties.getOrThrow(key: String) : String {
    val rawValue = requireNotNull(this.getProperty(key)) { "Can't find $key in properties file" }
    return resolvePlaceholders(rawValue)
  }

  val props = Properties().apply {
    // yswang 使用 use 块确保文件流被正确关闭
    //load(FileInputStream(path))
    File(path).inputStream().use { load(it) }
  }

  return SSLProperties(
    storeType = props.getOrThrow(SSL_STORE_TYPE),
    filePath = props.getOrThrow(SSL_FILE_PATH),
    storePassword = props.getOrThrow(SSL_STORE_PASSWORD),
    keyPassword = props.getOrThrow(SSL_KEY_PASSWORD)
  )
}

public fun setSsl(setWebSocketFactory: (WebSocketServerFactory) -> Unit): String? {
  val sslPropertiesFilePath = getOption(SSL_ENV_NAME) ?: return null

  try {
    val sslProperties = loadSSLProperties(sslPropertiesFilePath)

    val keyStore = KeyStore.getInstance(sslProperties.storeType).apply {
      // yswang 使用 use 块确保文件流被正确关闭
      //load(FileInputStream(sslProperties.filePath), sslProperties.storePassword.toCharArray())
      FileInputStream(sslProperties.filePath).use { stream ->
        load(stream, sslProperties.storePassword.toCharArray())
      }
    }

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509").apply {
      init(keyStore, sslProperties.keyPassword.toCharArray())
    }

    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509").apply {
      init(keyStore)
    }

    val sslContext = SSLContext.getInstance("TLS").apply {
      init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
    }

    setWebSocketFactory(DefaultSSLWebSocketServerFactory(sslContext))

    return sslPropertiesFilePath
  }
  catch (t: Throwable) {
    logger.info(t) { "Can't enable SSL from '$sslPropertiesFilePath' file" }

    return null
  }
}
