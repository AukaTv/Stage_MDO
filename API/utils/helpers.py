import logging
import os

def sql_row_to_snake(row):
    return {
        "num_palette": row["NumPalette"],
        "nom_client": row["NomClient"],
        "article": row["Article"],
        "quantite": row["Quantite"],
        "emplacement": row.get("Emplacement"),
    }

def get_logger(log_name: str):
    """
    Crée et retourne un logger configuré pour le fichier de log donné.
    """
    if not os.path.exists("logs"):
        os.makedirs("logs")

    logger = logging.getLogger(log_name)

    if not logger.hasHandlers():
        handler = logging.FileHandler(f"logs/{log_name}.log", encoding="utf-8")
        formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
        handler.setFormatter(formatter)
        logger.setLevel(logging.INFO)
        logger.addHandler(handler)
        logger.propagate = False

    return logger
