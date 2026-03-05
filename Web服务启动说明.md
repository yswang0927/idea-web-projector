1. 启动服务 `./bin/ide-projector-launcher.sh`

2. 浏览器访问: `http://localhost:8887` 

3. 使用不同端口启动: `./bin/ide-projector-launcher.sh --port=9887`

4. URL支持可选参数:

```
http://localhost:8887?
    backgroundColor=%23ffffff
    notSecureWarning=false
    projectPath=/data/my-project
    filePath=/data/my-project/src/A.java
    lineNumber=10
    theme=dark|light
```

5. 页面中暴露在 window 上的接口:

```
// 打开项目
window.projectorOpenProject(path)

// 打开文件
window.projectorOpenFile(filePath, line=0)

// 切换主题
window.projectorChangeTheme('dark|light')
```
