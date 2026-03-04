# 在线 Web-IDEA

## 投影服务器启动 idea

```shell
./bin/ide-projector-launcher.sh

# 或者打开指定的项目
./bin/ide-projector-launcher.sh ~/MyProject
```

## 在浏览器中打开

```
http://localhost:8887

# 支持传递参数
http://localhost:8887
  ? backgroundColor=%230000ff
  & projectPath=项目路径
  & notSecureWarning=true|false
  & repaintInterval=333
  & userScalingRatio=1.0
```

## 设置投影服务器用于 WebSocket 的端口

```
-Dorg.jetbrains.projector.server.port=8887
```

## 当web-idea进程启动后, 可以执行 `bin/idea.sh` 来进一步操作

```shell
# 打开项目
idea.sh ~/MyProject

# 打开文件定位到指定行
idea.sh --line 42 ~/MyProject/pom.xml

# 打开空白的差异查看器
idea.sh diff
idea.sh diff <path1> <path2> [<path3>]
# 比较两个文件
idea.sh diff ~/MyProject/Readme.md ~/MyProject/Readme.md.bak

# 从命令行格式化文件
idea.sh format [<options>] <path ...>
## 使用默认的代码样式设置格式化 ~/Data/src 目录中的两个特定文件：
idea.sh format -allowDefaults ~/Data/src/hello.html ~/Data/src/world.html
## 使用默认的代码样式设置递归格式化 ~/Data/src 目录（包括所有子目录）中的所有文件：
idea.sh format -allowDefaults -r ~/Data/src

```

## 使用 `idea.sh` 打开项目有时会询问是否信任

修改 `~/.config/JetBrains/idea202X.X/options/trusted_locations.xml` 文件配置信任路径:

```xml
<application>
  <component name="Trusted.Paths">
    <!-- 信任具体的项目 -->
    <option name="TRUSTED_PROJECT_PATHS">
      <map>
        <entry key="/data/Work/dev-workspaces/idea-workspace/ai-mcp-java" value="true" />
        <entry key="$USER_HOME$/IdeaProjects/my-springboot-scaffold" value="true" />
      </map>
    </option>
  </component>
  <component name="Trusted.Paths.Settings">
    <!-- 信任路径下的所有项目 -->
    <option name="TRUSTED_PATHS">
      <list>
        <option value="$USER_HOME$/IdeaProjects" />
        <option value="/data/Work/dev-workspaces/idea-workspace" />
      </list>
    </option>
  </component>
</application>
```

## 强制在当前窗口打开（跳过窗口选择）

修改 `~/.config/JetBrains/idea202X.X/options/ide.general.xml` 文件配置

```xml
<application>
  <component name="GeneralSettings">
    <option name="confirmOpenNewProject2" value="1" />
    <option name="showTipsOnStartup" value="false" />
  </component>
</application>
```
