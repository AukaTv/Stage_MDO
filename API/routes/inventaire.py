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

logger = get_logger("inventaire")

router = APIRouter()

# ----------------------------------------------------------

@router.get("")
def get_palettes_a_inventorier(user: User = Depends(get_current_user)):
    conn = get_connection()
    try:
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        cursor.execute("""
            SELECT NumPalette, Article, NomClient, Quantite, Emplacement
            FROM InfoPalette
            WHERE Statut = 'A Inventorier'
        """)
        results = cursor.fetchall()
        logger.info(f"Inventaire demandé par {getattr(user, 'username', user)}")
        # Conversion du format des clefs pour chaque résultat
        return [sql_row_to_snake(row) for row in results]
    except Exception as e:
        logger.error(f"Erreur dans get_palettes_a_inventorier : {e}")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        conn.close()

# ----------------------------------------------------------

@router.post("/valider_inventaire")
def valider_inventaire_liste(palettes: List[ValidationInventaire], user: User = Depends(get_current_user)):
    conn = get_connection()
    cursor = conn.cursor(pymysql.cursors.DictCursor)

    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    utilisateur = user.username if hasattr(user, "username") else user["username"]

    try:
        conn.begin()

        for data in palettes:
            cursor.execute("SELECT Statut FROM InfoPalette WHERE NumPalette = %s", (data.num_palette,))
            result = cursor.fetchone()
            if not result:
                logger.warning(f"Palette {data.num_palette} non trouvée en base par {utilisateur}")
                continue

            if data.quantite is not None and (not isinstance(data.quantite, int) or data.quantite < 0):
                logger.warning(f"Tentative de validation avec une quantité invalide : {data.quantite} par {utilisateur}")
                continue

            if result["Statut"] != "A Inventorier":
                logger.warning(f"Palette {data.num_palette} statut incorrect {result['Statut']} (attendu 'A Inventorier') par {utilisateur}")
                continue


            # Récupère l'ancien emplacement
            cursor.execute("SELECT Emplacement FROM InfoPalette WHERE NumPalette = %s", (data.num_palette,))
            row_old = cursor.fetchone()
            ancien_emplacement = row_old["Emplacement"] if row_old else None

            # Met à jour la palette avec le nouvel emplacement
            if data.quantite is not None:
                cursor.execute("""
                    UPDATE InfoPalette
                    SET Statut = %s, Date_Modif_Statut = %s,
                        Quantite = %s, Utilisateur_Modif_Statut = %s, Emplacement = %s
                    WHERE NumPalette = %s
                """, (data.statut, now, data.quantite, utilisateur, data.emplacement, data.num_palette))
            else:
                cursor.execute("""
                    UPDATE InfoPalette
                    SET Statut = %s, Date_Modif_Statut = %s, Utilisateur_Modif_Statut = %s, Emplacement = %s
                    WHERE NumPalette = %s
                """, (data.statut, now, utilisateur, data.emplacement, data.num_palette))

            # Libère l'ancien emplacement (s'il change et que l'ancien existe)
            if ancien_emplacement and ancien_emplacement != data.emplacement:
                cursor.execute("""
                    UPDATE EmplacementEntrepot
                    SET Etat = 'Libre'
                    WHERE Emplacement = %s
                """, (ancien_emplacement,))
            # Passe le nouvel emplacement en occupé
            if data.emplacement and (not ancien_emplacement or ancien_emplacement != data.emplacement):
                cursor.execute("""
                    UPDATE EmplacementEntrepot
                    SET Etat = 'Occupé'
                    WHERE Emplacement = %s
                """, (data.emplacement,))

            logger.info(f"Palette {data.num_palette} validée par {utilisateur} avec statut {data.statut} et quantité {data.quantite}")

            cursor.execute("""
                INSERT INTO STT_Palette (NumPalette, Statut, Date_Modif_Statut, Utilisateur_Modif_Statut)
                VALUES (%s, %s, %s, %s)
            """, (data.num_palette, data.statut, now, utilisateur))

        conn.commit()
    except Exception as e:
        conn.rollback()
        logger.error(f"Erreur lors de la validation des palettes par {utilisateur} : {e}")
        raise HTTPException(status_code=500, detail="Erreur lors de la validation")
    finally:
        conn.close()

    return {"message": f"{len(palettes)} palettes validées"}