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
package org.jetbrains.projector.client.web.protocol

import org.jetbrains.projector.common.protocol.compress.MessageDecompressor
import org.jetbrains.projector.common.protocol.handshake.CompressionType

object GZipToClientMessageDecompressor : MessageDecompressor<ByteArray> {

  override fun decompress(data: ByteArray): ByteArray {
    // yswang add: 修正 enableCompression 参数以使用 gzip
    // 主要原因是 Java 压缩代码是 int8，但 pako.inflate 需要 uint8。
    // see https://github.com/JetBrains/projector-client/pull/152/changes
    //return pako.inflate(data) as ByteArray
    return pako(data)
  }

  override val compressionType = CompressionType.GZIP

  // yswang change to:
  //private val pako: dynamic = js("window.pako")
  private val pako: dynamic = js("window.pakoDepress")
}
