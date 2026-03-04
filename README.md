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
