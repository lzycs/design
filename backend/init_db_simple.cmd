@echo off
mysql -u root -p -e "DROP DATABASE IF EXISTS campus_learning_space;"
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS campus_learning_space DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p campus_learning_space < src\main\resources\sql\init_simple.sql
