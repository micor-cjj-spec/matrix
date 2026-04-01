@echo off
REM 创建集中存放JAR包的目录
set targetDir=all-jars
if not exist %targetDir% (
    mkdir %targetDir%
)

REM 递归查找所有target目录下的JAR包并复制到all-jars目录
for /r %%i in (*.jar) do (
    copy "%%i" "%targetDir%\"
)

echo 所有jar包已复制到 %targetDir% 目录下！
pause
