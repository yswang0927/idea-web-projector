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

6. 以HTTPS服务启动
- 准备 SSL证书文件 `projector.p12` (可以使用 openssl 创建自签名证书) 放到 `projector-server/` 目录下.
- 在 `bin/` 目录下创建 `ssl.properties` 配置文件, 内容如下:
```
STORE_TYPE=PKCS12
FILE_PATH=${IDE_HOME}/projector-server/projector.p12
STORE_PASSWORD=projector@2026
KEY_PASSWORD=projector@2026
```
- 或者通过 `export IDE_SERVER_SSL_CONFIG_PATH="/path/ssl.properties"` 来自己指定SSL配置文件路径.
