#!/bin/bash

# search for IDE runner sh file:

# yswang add
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
PRGDIR=`dirname "$PRG"`

IDE_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`

#---- yswang auto copy eula accepted file ----
TARGET_DIR="$HOME/.java/.userPrefs/jetbrains/"
EUA_DIR="$IDE_HOME/eua"

mkdir -p "$TARGET_DIR"
if [ -d "$EUA_DIR" ]; then
  if [ ! -f "$TARGET_DIR/idea-web-eua.txt" ]; then
    echo ">> Initializing IDEA-web EULA and preferences..."
    cp -a "$EUA_DIR/." "$TARGET_DIR"
    chown -R "$(id -u):$(id -g)" "$TARGET_DIR"
  fi
fi

#---- yswang disable send data ----
CONSENT_DIR_SHARE="$HOME/.local/share/JetBrains/consentOptions"
mkdir -p "$CONSENT_DIR_SHARE"
if [ ! -f "$CONSENT_DIR_SHARE/accepted" ]; then
  TIMESTAMP=$(date +%s000)
  CONSENT_STRING="rsch.send.usage.stat:1.1:0:$TIMESTAMP"
  echo -n "$CONSENT_STRING" > "$CONSENT_DIR_SHARE/accepted"
  echo ">> Data Sharing dialog suppressed successfully."
fi
#----------------------------------------------------------------

THIS_FILE_NAME=$(basename "$0")

# --- 1. 参数解析逻辑 (新增) ---
# 默认端口号
port=8887

for i in "$@"; do
  case $i in
    --port=*)
      port="${i#*=}"
      shift # 从参数列表中移除 --port
      ;;
    *)
      # 其他参数保留给 IDE
      ;;
  esac
done
# ----------------------------

ideRunnerCandidates=($(grep -lr --include=*.sh "com.intellij.idea.Main\|jetbrains.mps.Launcher" .))

# remove this file from candidates:
for i in "${!ideRunnerCandidates[@]}"; do
    if [[ ${ideRunnerCandidates[i]} = *$THIS_FILE_NAME* ]]; then
        unset 'ideRunnerCandidates[i]'
    elif [[ ${ideRunnerCandidates[i]} = *"projector"* ]]; then
        unset 'ideRunnerCandidates[i]'
    elif [[ ${ideRunnerCandidates[i]} = *"game-tools.sh" ]]; then
        unset 'ideRunnerCandidates[i]'
    fi
done

if [[ ${#ideRunnerCandidates[@]} != 1 ]]; then
    echo "Can't find a single candidate to be IDE runner script so can't select a single one:"
    echo ${ideRunnerCandidates[*]}
    exit 1
fi

ideRunnerCandidate=${ideRunnerCandidates[@]}
ideRunnerWithoutPrefix=${ideRunnerCandidate/"./"/""}
IDE_RUN_FILE_NAME=${ideRunnerWithoutPrefix/".sh"/""}
echo "Found IDE: $IDE_RUN_FILE_NAME"

# yswang add
PROJECTOR_PROPS="-DIDE_HOME=${IDE_HOME} -Dorg.jetbrains.projector.server.port=${port}"
SSL_FILE="${IDE_HOME}/bin/ssl.properties"
# 先检测外部环境变量是否传递了ssl配置文件
if [ -n "$IDE_SERVER_SSL_FILE_PATH" ]; then
  PROJECTOR_PROPS="${PROJECTOR_PROPS} -DORG_JETBRAINS_PROJECTOR_SERVER_SSL_PROPERTIES_PATH=${IDE_SERVER_SSL_FILE_PATH}"
elif [ -f "${SSL_FILE}" ]; then
  PROJECTOR_PROPS="${PROJECTOR_PROPS} -DORG_JETBRAINS_PROJECTOR_SERVER_SSL_PROPERTIES_PATH=${SSL_FILE}"
fi

cp "$IDE_RUN_FILE_NAME.sh" "$IDE_RUN_FILE_NAME-projector.sh"

# change
# classpath "$CLASSPATH"
# to
# classpath "$CLASSPATH:$IDE_HOME/projector-server/lib/*"
sed -i 's+classpath "$CLASSPATH"+classpath "$CLASSPATH:$IDE_HOME/projector-server/lib/*"+g' "$IDE_RUN_FILE_NAME-projector.sh"

# change
# com.intellij.idea.Main
# to
# -Dorg.jetbrains.projector.server.classToLaunch=com.intellij.idea.Main org.jetbrains.projector.server.ProjectorLauncher
sed -i "s+com.intellij.idea.Main+${PROJECTOR_PROPS} -Dorg.jetbrains.projector.server.classToLaunch=com.intellij.idea.Main org.jetbrains.projector.server.ProjectorLauncher+g" "$IDE_RUN_FILE_NAME-projector.sh"

# change
# ${MAIN_CLASS}
# to
# -Dorg.jetbrains.projector.server.classToLaunch=${MAIN_CLASS} org.jetbrains.projector.server.ProjectorLauncher
#sed -i 's+\${MAIN_CLASS}+-Dorg.jetbrains.projector.server.classToLaunch=\${MAIN_CLASS} org.jetbrains.projector.server.ProjectorLauncher+g' "$IDE_RUN_FILE_NAME-projector.sh"

bash "$IDE_RUN_FILE_NAME-projector.sh" "$@"

rm "$IDE_RUN_FILE_NAME-projector.sh"
