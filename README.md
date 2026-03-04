# 在线 Web 版本IDEA

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

- `projector-client/projector-client-common/src/jsMain/kotlin/org/jetbrains/projector/client/common/misc/ParamsProvider.kt`
```
// yswang 声明新变量
val PROJECT_PATH: String? 
val FILE_PATH: String?
val FILE_LINE: Int?

// yswang add 接收要打开的项目路径和文件路径(:8887?projectPath=&filePath=&lineNumber=)
PROJECT_PATH = searchParams.get("projectPath") ?: DEFAULT_PROJECT_PATH
FILE_PATH = searchParams.get("filePath") ?: ""
FILE_LINE = searchParams.get("lineNumber")?.toIntOrNull() ?: 0
```

- `projector-client/projector-common/src/commonMain/kotlin/org/jetbrains/projector/common/protocol/toServer/ClientEvent.kt`
```
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
```

- `projector-server/projector-server-common/src/main/kotlin/org/jetbrains/projector/server/ProjectorServer.kt`
```
import com.intellij.ide.impl.*
import com.intellij.openapi.application.*

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
```

- `projector-client/projector-client-web/src/main/kotlin/org/jetbrains/projector/client/web/window/WebWindowManager.kt`
```
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
```
