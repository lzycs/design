@echo off
mysql -u root -p --default-character-set=utf8mb4 < "src\main\resources\sql\init.sql"
