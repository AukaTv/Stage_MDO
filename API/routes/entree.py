from fastapi import APIRouter, HTTPException
from models.palette import EntreePalette, SortiePaletteComplete, ValidationInventaire
from models.user import User
from db import get_connection
from datetime import datetime
from fastapi import Depends
from auth import get_current_user
from typing import List
import pymysql.cursors
from utils.helpers import sql_row_to_snake, get_logger

router = APIRouter()
logger = get_logger("entree")

# ----------------------------------------------------------

@router.get("/palette/{num_palette}")
def get_palette_infos(num_palette: str, user: dict = Depends(get_current_user)):
    logger.info(f"Recherche palette : {num_palette}")
    conn = get_connection()
    try:
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        cursor.execute("""
            SELECT NumPalette, NomClient, Article, Quantite, Emplacement
            FROM InfoPalette
            WHERE NumPalette = %s
        """, (num_palette,))
        result = cursor.fetchone()
        if not result:
            logger.warning("Palette non trouvée en base")
            raise HTTPException(status_code=404, detail="Palette non trouvée")
        data = sql_row_to_snake(result)
        logger.info(f"Palette trouvée : {data}")
        return data
    except Exception as e:
        logger.error(f"Erreur dans get_palette_infos : {e}")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        conn.close()

# ----------------------------------------------------------

@router.post("")
def entree_palette(data: EntreePalette, user: dict = Depends(get_current_user)):
    data.num_palette = data.num_palette.strip()
    data.emplacement = data.emplacement.strip()
    logger.info("ROUTE palette/entree appelée")
    conn = get_connection()
    cursor = conn.cursor(pymysql.cursors.DictCursor)

    try:
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        statut = "En stock"

        # Vérification existence palette
        logger.info(f"Vérification existence palette : {data.num_palette}")
        cursor.execute("SELECT COUNT(*) AS count FROM InfoPalette WHERE NumPalette = %s", (data.num_palette,))
        count = cursor.fetchone()["count"]
        logger.info(f"Nombre de palettes trouvées : {count}")
        if count == 0:
            raise HTTPException(status_code=404, detail="Palette non trouvée")

        # Récupération de l'ancien emplacement de la palette
        logger.info(f"Récupération de l'ancien emplacement pour la palette : {data.num_palette}")
        cursor.execute("SELECT Emplacement FROM InfoPalette WHERE NumPalette = %s", (data.num_palette,))
        old_emplacement_result = cursor.fetchone()
        old_emplacement = old_emplacement_result["Emplacement"] if old_emplacement_result else None
        logger.info(f"Ancien emplacement : {old_emplacement}, Nouvel emplacement : {data.emplacement}")

        # Mise à jour de l'ancien emplacement à 'Libre' si différent du nouvel emplacement
        if old_emplacement and old_emplacement != data.emplacement:
            logger.info(f"Mise à jour de l'ancien emplacement '{old_emplacement}' à 'Libre'")
            cursor.execute("""
                UPDATE EmplacementEntrepot
                SET Etat = %s
                WHERE emplacement = %s
            """, ("Libre", old_emplacement))

        # Mise à jour InfoPalette avec le nouvel emplacement et statut
        logger.info(f"Update InfoPalette : emplacement={data.emplacement} statut={statut}, user={user['username']}, num_palette={data.num_palette}")
        cursor.execute("""
            UPDATE InfoPalette
            SET Emplacement = %s, Statut = %s,
            Date_Modif_Statut = %s, Utilisateur_Modif_Statut = %s, Date_Dernier_MVT = %s
            WHERE NumPalette = %s
        """, (data.emplacement, statut, now, user["username"], now, data.num_palette))

        # Insertion dans MVT_Palette
        logger.info(f"Insertion MVT_Palette : num_palette={data.num_palette}, now={now}, zone=Entrepôt")
        cursor.execute("""
            INSERT INTO MVT_Palette (NumPalette, Date_Dernier_MVT, Zone)
            VALUES (%s, %s, %s)
        """, (data.num_palette, now, "Entrepôt"))

        # Insertion dans STT_Palette
        logger.info(f"Insertion STT_Palette : num_palette={data.num_palette}, statut={statut}, now={now}, user={user['username']}")
        cursor.execute("""
            INSERT INTO STT_Palette (NumPalette, Statut, Date_Modif_Statut, Utilisateur_Modif_Statut)
            VALUES (%s, %s, %s, %s)
        """, (data.num_palette, statut, now, user["username"]))

        # Vérification existence du nouvel emplacement dans EmplacementEntrepot
        logger.info(f"Vérification existence du nouvel emplacement '{data.emplacement}' dans EmplacementEntrepot")
        cursor.execute("""
            SELECT COUNT(*) AS count FROM EmplacementEntrepot WHERE emplacement = %s
        """, (data.emplacement,))
        result = cursor.fetchone()
        emplacement_exists = result["count"] if result else 0

        # Mise à jour ou insertion du nouvel emplacement en 'Occupé'
        if emplacement_exists:
            logger.info(f"Mise à jour de l'état du nouvel emplacement '{data.emplacement}' à 'Occupé'")
            cursor.execute("""
                UPDATE EmplacementEntrepot
                SET Etat = %s
                WHERE emplacement = %s
            """, ("Occupé", data.emplacement))
        else:
            logger.info(f"Insertion du nouvel emplacement '{data.emplacement}' avec état 'Occupé'")
            cursor.execute("""
                INSERT INTO EmplacementEntrepot (emplacement, Etat)
                VALUES (%s, %s)
            """, (data.emplacement, "Occupé"))

        # Validation de la transaction
        conn.commit()
        logger.info("Transaction validée avec succès")
    except Exception as e:
        conn.rollback()
        logger.error(f"Erreur lors de l'enregistrement en base: {e}")
        raise HTTPException(status_code=500, detail=f"Erreur lors de l'enregistrement en base: {e}")
    finally:
        conn.close()

    return {"message": "Entrée en stock enregistrée avec succès"}
