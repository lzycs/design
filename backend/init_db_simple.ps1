# 执行MySQL命令创建数据库和表
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS campus_learning_space DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p campus_learning_space -e "source src/main/resources/sql/init.sql;"
