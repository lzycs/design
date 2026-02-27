# 定义MySQL命令
$mysqlCommand = "mysql"
$mysqlArgs = @(
    "-u", "root",
    "-p",
    "--default-character-set=utf8mb4",
    "-e", "CREATE DATABASE IF NOT EXISTS campus_learning_space DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
)

# 运行MySQL命令
Start-Process -FilePath $mysqlCommand -ArgumentList $mysqlArgs -Wait -NoNewWindow

# 再次运行MySQL命令，执行SQL脚本
$mysqlArgs2 = @(
    "-u", "root",
    "-p",
    "--default-character-set=utf8mb4",
    "campus_learning_space",
    "-e", "source src/main/resources/sql/init.sql;"
)

Start-Process -FilePath $mysqlCommand -ArgumentList $mysqlArgs2 -Wait -NoNewWindow
