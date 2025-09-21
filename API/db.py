import os
import pymysql
from dotenv import load_dotenv
import sys
import traceback
import logging

sys.excepthook = lambda exctype, value, tb: open("error_log.txt", "a", encoding="utf-8").write("\n".join([
    "=== Exception non interceptée ===",
    *traceback.format_exception(exctype, value, tb)
]))

def resource_path(filename):
    return os.path.join(os.path.dirname(sys.executable if getattr(sys, 'frozen', False) else __file__), "data", filename)

load_dotenv(resource_path(".env"))

mysql_user = os.getenv("MYSQL_USER")
mysql_password = os.getenv("MYSQL_PASSWORD")
mysql_host = os.getenv("MYSQL_HOST")
mysql_port = int(os.getenv("MYSQL_PORT", "3306"))
mysql_bname = os.getenv("MYSQL_BNAME")

def get_connection():
    try:
        return pymysql.connect(
            host=mysql_host,
            port=mysql_port,
            user=mysql_user,
            password=mysql_password,
            database=mysql_bname,
            charset="utf8mb4",
            cursorclass=pymysql.cursors.DictCursor
        )
    except Exception as e:
        logging.error(f"Erreur de connexion MySQL : {e}")
        sys.exit(1)
