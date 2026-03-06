# 在线 Web 版本IDEA(升级支持到2021.3)

> 
> 基于jetbrains开源的 `projector-server` 和 `projector-client` 进行定制修改.
> 
> https://github.com/JetBrains/projector-server
> 
> https://github.com/JetBrains/projector-client
> 


## 1. 自己编译打包步骤

- 先修改 `projector-client/gradle.properties` 和 `projector-server/gradle.properties` 文件的配置项:
```
# 修改为本地的jdk11
org.gradle.java.installations.paths=/data/Work/DevEnv/jdk11
```

- 进入 `projector-server` 目录下执行编译打包命令, 编译打包成功后会生成文件 `projector-server/build/distibution/projector-server-<version>.zip`
```shell
./gradlew clean
./gradlew :projector-server:distZip
```

## 2. 集成 IDEA-2021.3.3

- 下载IDEA包: `https://download.jetbrains.com/idea/ideaIC-2021.3.3.tar.gz`
- 解压IDEA-2021.3.3, 将 `projector-server/build/distibution/projector-server-<version>.zip` 解压到IDEA-2021.3.3目录下:
```
idea-2021.3.3
  |-bin
     |-ide-projector-launcher.sh
  |-jbr
  |-lib
  |-projector-server
      |-bin
      |-lib
```
- 将 `ide-projector-launcher.sh` 文件拷贝到 `idea-2021.3.3/bin` 目录下
- 运行 `idea-2021.3.3/bin/ide-projector-launcher.sh` 启动web服务,默认端口 `8887`
- 浏览器访问 `http://localhost:8887` 即可

## 3. 更多命令

- 指定项目路径启动: `./bin/ide-projector-launcher.sh ~/MyProject`, 启动后自动打开此项目.
- 通过URL切换项目: `http://localhost:8887?projectPath=/your-project-path`
- 通过URL打开项目文件: `http://localhost:8887?filePath=/your-project-path/A.java&lineNumber=10`, 参数 `lineNumber` 可选.
- 通过js方法打开切换项目, 在 `http://localhost:8887` 页面的 `window` 对象上暴露了 `projectorOpenProject(path)` 和 `projectorOpenFile(path, line)` 方法:

## 4. 定制修改的地方

- `projector-client/projector-client-web/src/main/resources/index.html`
```html
<script>
  // iframe 内部接收
  const params = new URLSearchParams(window.location.search);
  let paramProjectPath = params.get('projectPath') || '';
  let paramFilePath = params.get('filePath') || '';
  let paramLineNo = params.get('lineNumber') || 0;
  let paramTheme = params.get('theme') || '';

  let isMainWindowReady = false;
  // 当主窗口准备好后, 自动处理需要打开的指令操作
  window.addEventListener('projectorWindowReady', function() {
    if (!isMainWindowReady) {
      isMainWindowReady = true;
      if (paramProjectPath) {
        setTimeout(function(){
          window.projectorOpenProject(paramProjectPath);
        }, 300);
      }

      if (paramFilePath) {
        setTimeout(function(){
          window.projectorOpenFile(paramFilePath, paramLineNo);
        }, 300);
      }

      if (paramTheme) {
        setTimeout(function(){
          window.projectorChangeTheme(paramTheme);
        }, 300);
      }

      // 手工触发下窗口resize事件,让IDEA自动适应浏览器窗口大小
      setTimeout(function(){
        window.dispatchEvent(new Event('resize'));
      }, 300);
    }
  });

  window.addEventListener('message', (event) => {
    // {'action':'openProject', 'value': '/path'}
    // {'action':'openFile', 'value': '/path/file'}
    const msg = event.data;
    if (typeof msg !== 'object' || msg === null) return;
    const action = msg.action || 'unknown';
    const value = msg.value || '';

    if (!isMainWindowReady) {
      console.warn(">> 主窗口未准备好, 不能立刻处理事件: ", msg);
    }

    switch (action) {
      // 打开项目
      case 'openProject':
        if (value && typeof value === 'string') {
          paramProjectPath = value;
          isMainWindowReady && window.projectorOpenProject(value);
        }
        break;
      // 打开文件,支持行号: /A.java#L10
      case 'openFile':
        if (value && typeof value === 'string') {
          let filePath = value;
          let lineNo = 0;
          const lineIndex = value.indexOf('#L');
          if (lineIndex != -1) {
            filePath = value.substring(0, lineIndex);
            lineNo = parseInt(value.substring(lineIndex + 2)) || 0;
          }
          if (filePath) {
            // 相对路径, 需要拼接上项目路径
            if (filePath.charAt(0) != '/') {
              if (!paramProjectPath) {
                console.warn('>> 无项目路径, 无法打开相对路径文件: '+ filePath);
                return;
              }
              filePath = paramProjectPath + (paramProjectPath.charAt(paramProjectPath.length - 1) != '/' ? '/' : '') + filePath;
            }
            paramFilePath = filePath;
            paramLineNo = Math.max(0, lineNo);
            isMainWindowReady && window.projectorOpenFile(filePath, Math.max(0, lineNo));
          }
        }
        break;
      // 切换主题
      case 'theme':
        if (value && typeof value === 'string') {
          paramTheme = value;
          isMainWindowReady && window.projectorChangeTheme(value);
        }
        break;
      default:
        console.error('>> 无效的命令: ', msg);
    }
  });
</script>
```

- `projector-client/projector-client-web/src/main/kotlin/org/jetbrains/projector/client/web/state/ClientState.kt`
```kotlin
// 进行了部分信息汉化
// yswang add: 在创建新连接前关闭旧的 websocket
// https://github.com/JetBrains/projector-client/pull/156/changes
webSocket.onclose = null
webSocket.close()
```

- `projector-client/projector-client-common/src/jsMain/kotlin/org/jetbrains/projector/client/common/misc/ParamsProvider.kt`
```kotlin
// yswang 声明新变量
val PROJECT_PATH: String? 
val FILE_PATH: String?
val FILE_LINE: Int?
val THEME: String?

// yswang add 接收要打开的项目路径和文件路径(:8887?projectPath=&filePath=&lineNumber=)
PROJECT_PATH = searchParams.get("projectPath") ?: DEFAULT_PROJECT_PATH
FILE_PATH = searchParams.get("filePath") ?: ""
FILE_LINE = searchParams.get("lineNumber")?.toIntOrNull() ?: 0
THEME = searchParams.get("theme") ?: ""
```

- `projector-client/projector-common/src/commonMain/kotlin/org/jetbrains/projector/common/protocol/toServer/ClientEvent.kt`
```kotlin
// 新增自定义 websocket 指令
// yswang add 添加一个打开项目的事件
@Serializable
data class ClientOpenProjectEvent(
  val projectPath: String,
) : ClientEvent()

@Serializable
data class ClientOpenFileEvent(
  val filePath: String,
  val line: Int = 0 // 可选：指定跳转行号
) : ClientEvent()

@Serializable
data class ClientChangeThemeEvent(
  val theme: String,
) : ClientEvent()
```

- `projector-server/projector-server-common/src/main/kotlin/org/jetbrains/projector/server/ProjectorServer.kt`
```kotlin
import com.intellij.ide.impl.*
import com.intellij.openapi.application.*

// 凡是修改的地方都有 yswang 标记

// yswang add 接收到客户端发送的打开项目指令
is ClientOpenProjectEvent -> {
    val path = message.projectPath
    logger.info { ">> Received request from client to open project: $path" }
    // 使用 IntelliJ 平台的 Application 实例
    val application = ApplicationManager.getApplication()
    application.invokeLater({
      try {
        val projectDir = Paths.get(path).toAbsolutePath().normalize()
    
        // 是否打开的是同一个项目
        var existingProject = ProjectUtil.findAndFocusExistingProjectForPath(projectDir)
        if (existingProject != null) {
          //logger.info { ">> Projector: Project is already open, skipping: ${existingProject.getName()}" }
          return@invokeLater // 在 lambda 中使用 return@invokeLater
        }
    
        // 自动信任
        val trustedPaths = TrustedPaths.getInstance()
        val parentDir = projectDir.parent
        if (parentDir != null) {
          val trustedState = trustedPaths.getProjectPathTrustedState(parentDir)
          if (trustedState != com.intellij.util.ThreeState.YES) {
            trustedPaths.setProjectPathTrusted(parentDir, true)
            logger.info { ">> Projector: Trusted parent directory: $parentDir" }
          }
        } else {
          // 如果没有父目录（极少见），则只信任项目本身
          trustedPaths.setProjectPathTrusted(projectDir, true)
        }
    
        // 使用 2021 版 ProjectUtil.openOrImport 的正确重载
        // https://github.com/JetBrains/intellij-community/blob/idea/221.6008.13/platform/platform-impl/src/com/intellij/ide/impl/ProjectUtil.java
        val openedProjects = ProjectUtil.getOpenProjects()
        // 寻找需要关闭的项目（通常是第一个）
        val projectToClose = if (openedProjects.isNotEmpty()) openedProjects[0] else null
        // openOrImport(java.nio.file.Path path, Project projectToClose, boolean forceOpenInNewFrame) 
        val openedProject = ProjectUtil.openOrImport(projectDir, projectToClose, false)
        if (openedProject != null) {
          logger.info { ">> Projector: Successfully opened project: $path" }
        } else {
          logger.info { ">> Projector: Project opening was cancelled or failed: $path" }
        }
    
      } catch (e: Exception) {
        logger.error(e) { ">> Projector: Error opening project: $path" }
      }
    }, ModalityState.defaultModalityState())
}

// yswang add 接收到打开文件的指令
is ClientOpenFileEvent -> {
    val filePath = message.filePath
    val line = message.line
    logger.info { ">> Projector: Received request to open file: $filePath" }
    
    ApplicationManager.getApplication().invokeLater({
      try {
        val openedProjects = ProjectUtil.getOpenProjects()
        val project = if (openedProjects.isNotEmpty()) openedProjects[0] else null
        if (project == null) {
          logger.info { ">> Projector: No project opened. Cannot open file: $filePath" }
          return@invokeLater
        }
    
        val ioPath = Paths.get(filePath).toAbsolutePath().normalize()
        val virtualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByPath(ioPath.toString())
        if (virtualFile == null) {
          logger.info { ">> Projector: File not found: $filePath" }
          return@invokeLater
        }
    
        // 有行号就跳转，没有就直接打开
        val descriptor = if (line > 0) {
          com.intellij.openapi.fileEditor.OpenFileDescriptor(project, virtualFile, line - 1, 0)
        } else {
          com.intellij.openapi.fileEditor.OpenFileDescriptor(project, virtualFile)
        }
        if (descriptor.canNavigate()) {
          descriptor.navigate(true)
        }
    
        logger.info { ">> Projector: Successfully opened file: $filePath" }
      } catch (e: Exception) {
        logger.error(e) { ">> Projector: Error opening file: $filePath" }
      }
    }, ModalityState.defaultModalityState())
}

// 切换主题
is ClientChangeThemeEvent -> {
  val themeName = when {
    message.theme.equals("dark", ignoreCase = true) -> "Darcula"
    message.theme.equals("light", ignoreCase = true) -> "IntelliJ Light"
    else -> message.theme  // 支持直接传主题原名
  }

  ApplicationManager.getApplication().invokeLater({
    val lafManager = com.intellij.ide.ui.LafManager.getInstance()
    val lookFeel = lafManager.installedLookAndFeels.find { it.name.contains(themeName, ignoreCase = true) }
    if (lookFeel != null) {
      lafManager.setCurrentLookAndFeel(lookFeel)
      lafManager.updateUI()
      logger.info { ">> Projector: Theme changed to $themeName" }
    }
  }, ModalityState.NON_MODAL)
}
```

- `projector-client/projector-client-web/src/main/kotlin/org/jetbrains/projector/client/web/window/WebWindowManager.kt`
```kotlin
init {
    window.onblur = ::onDeactivated
    window.onfocus = ::onActivated

    // --- yswang 新增逻辑 ---
    // --- 将方法暴露给全局 JS (window 对象) ---
    // 这样你在浏览器控制台输入 window.projectorOpenProject(...) 就能调用
    (window.asDynamic()).projectorOpenProject = ::openProject
    (window.asDynamic()).projectorOpenFile = ::openFile
    (window.asDynamic()).projectorChangeTheme = ::changeTheme

    // 检查 URL 参数启动参数
    ParamsProvider.PROJECT_PATH?.let { openProject(it) }
    ParamsProvider.FILE_PATH?.let { openFile(it, ParamsProvider.FILE_LINE ?: 0) }
    ParamsProvider.THEME?.let { changeTheme(it) }
    // ----------------
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

/**
 * 切换主题
 */
fun changeTheme(themeName: String) {
  if (themeName == null || themeName.isBlank()) return
  stateMachine.fire(ClientAction.AddEvent(ClientChangeThemeEvent(theme = themeName)))
}
```

- `projector-server/projector-server-common/src/main/kotlin/org/jetbrains/projector/server/idea/CaretInfoUpdater.kt`
```kotlin
// 修改的地方都标记了 yswang
private fun getCurrentEditorImpl(): EditorImpl? {
  val dataContext = try {
    myDataManager.dataContextFromFocusAsync.blockingGet(DATA_CONTEXT_QUERYING_TIMEOUT_MS)
  } catch (e : TimeoutException) {
    null
  } ?: return null

  // yswang IDEA-2021.3.x 后更严格了
  //return readAction { dataContext.getData(CommonDataKeys.EDITOR) } as? EditorImpl
  // 将 readAction 替换为 invokeAndWaitIfNeeded，强制在 EDT 中获取 DataContext 数据
  return invokeAndWaitIfNeeded { 
    dataContext.getData(CommonDataKeys.EDITOR) as? EditorImpl 
  }
}
```

- `projector-client/projector-server-core/src/main/kotlin/org/jetbrains/projector/server/core/websocket/HttpWsServer.kt`
```kotlin
override fun forEachOpenedConnection(action: (client: ClientWrapper) -> Unit) {
  // yswang 修复 java.lang.ClassCastException: class org.jetbrains.projector.server.core.websocket.HttpWsServer$Companion$HTTP_CONNECTION_ATTACHMENT$1 
  //  cannot be cast to class org.jetbrains.projector.server.core.ClientWrapper 错误
  /* 
  webSocketServer.connections.filter(WebSocket::isOpen).forEach {
    val wrapper = it.getAttachment<ClientWrapper>() ?: return@forEachOpenedConnection
    action(wrapper)
  }*/
  webSocketServer.connections.filter(WebSocket::isOpen).forEach {
    // 先以 Any? 类型安全地获取附加对象，避免底层抛出转换异常
    val attachment = it.getAttachment<Any?>()
    // 使用 as? 进行安全的类型检查，如果不是 ClientWrapper 就会返回 null，从而安全跳过
    val wrapper = attachment as? ClientWrapper ?: return@forEachOpenedConnection
    action(wrapper)
  }
}
```

- `projector-client/projector-client-web/src/main/kotlin/org/jetbrains/projector/client/web/window/WindowDataEventsProcessor.kt`
```kotlin
fun process(windowDataEvents: ServerWindowSetChangedEvent) {
  // ...other...

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
```