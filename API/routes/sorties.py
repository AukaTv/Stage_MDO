from fastapi import APIRouter, HTTPException
from models.palette import EntreePalette, SortiePaletteComplete, ValidationDestruction, ValidationRenvoie, ValidationSortieProduction
from models.user import User
from db import get_connection
from datetime import datetime
from fastapi import Depends
from auth import get_current_user
from typing import List
import logging
import os
import pymysql.cursors
from utils.helpers import sql_row_to_snake

router = APIRouter()

@router.get("/destruction")
def get_palettes_a_detruire(user: User = Depends(get_current_user)):
    conn = get_connection()
    try:
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        cursor.execute("""
            SELECT NumPalette, Article, NomClient, Quantite, Emplacement
            FROM InfoPalette
            WHERE Statut = 'A Détruire'
        """)
        results = cursor.fetchall()
        logging.info(f"Destruction demandé par {getattr(user, 'username', user)}")
        return [sql_row_to_snake(row) for row in results]
    except Exception as e:
        logging.error(f"Erreur dans get_palettes_a_detruire : {e}")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        conn.close()

# ----------------------------------------------------------

@router.post("/valider_destruction")
def valider_destruction_liste(palettes: List[ValidationDestruction], user: User = Depends(get_current_user)):
    conn = get_connection()
    cursor = conn.cursor(pymysql.cursors.DictCursor)

    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    utilisateur = getattr(user, "username", user.get("username", "inconnu"))

    try:
        conn.begin()
        valid_count = 0

        for data in palettes:
            # Vérifie l'existence et statut de la palette
            cursor.execute("SELECT Statut, Emplacement FROM InfoPalette WHERE NumPalette = %s", (data.num_palette,))
            result = cursor.fetchone()
            emplacement = result.get("Emplacement")
            if not result:
                logging.warning(f"Palette {data.num_palette} non trouvée, ignorée")
                continue
            if result["Statut"] != "A Détruire":
                logging.warning(f"Palette {data.num_palette} avec statut {result['Statut']} non conforme, ignorée")
                continue

            # Mise à jour statut
            cursor.execute("""
                UPDATE InfoPalette
                SET Statut = 'Détruite', Date_Modif_Statut = %s, Utilisateur_Modif_Statut = %s
                WHERE NumPalette = %s
            """, (now, utilisateur, data.num_palette))

            # Trace historique
            cursor.execute("""
                INSERT INTO STT_Palette (NumPalette, Statut, Date_Modif_Statut, Utilisateur_Modif_Statut)
                VALUES (%s, %s, %s, %s)
            """, (data.num_palette, "Détruite", now, utilisateur))

            #Trace mouvement
            cursor.execute("""
                INSERT INTO MVT_Palette (NumPalette, Date_Dernier_MVT, Zone)
                VALUES (%s, %s, %s)
            """, (data.num_palette, now, "Détruit"))

            #Liberation de l'emplacement
            cursor.execute("""
                UPDATE EmplacementEntrepot
                SET Etat = 'Libre'
                WHERE Emplacement = %s
            """, (emplacement,))
            valid_count += 1

        conn.commit()
        logging.info(f"{valid_count} palettes validées pour destruction par {utilisateur}")
        return {"message": f"{valid_count} palettes validées pour destruction"}

    except Exception as e:
        conn.rollback()
        logging.error(f"Erreur lors de la validation des destructions par {utilisateur} : {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Erreur lors de la validation")
    finally:
        conn.close()

# ----------------------------------------------------------

@router.get("/renvoie")
def get_palettes_a_renvoyer(user: User = Depends(get_current_user)):
    conn = get_connection()
    try:
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        cursor.execute("""
            SELECT NumPalette, Article, NomClient, Quantite, Emplacement
            FROM InfoPalette
            WHERE Statut = 'A Renvoyer'
        """)
        results = cursor.fetchall()
        logging.info(f"Renvoie demandé par {getattr(user, 'username', user)}")
        return [sql_row_to_snake(row) for row in results]
    except Exception as e:
        logging.error(f"Erreur dans get_palettes_a_renvoyer : {e}")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        conn.close()

# ----------------------------------------------------------

@router.post("/valider_renvoie")
def valider_renvoie_liste(palettes: List[ValidationRenvoie], user: User = Depends(get_current_user)):
    conn = get_connection()
    cursor = conn.cursor(pymysql.cursors.DictCursor)

    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    utilisateur = getattr(user, "username", user.get("username", "inconnu"))

    try:
        conn.begin()
        valid_count = 0

        for data in palettes:
            cursor.execute("SELECT Statut, Emplacement FROM InfoPalette WHERE NumPalette = %s", (data.num_palette,))
            result = cursor.fetchone()
            emplacement = result.get("Emplacement")
            if not result:
                logging.warning(f"Palette {data.num_palette} non trouvée, ignorée")
                continue
            if result["Statut"] != "A Renvoyer":
                logging.warning(f"Palette {data.num_palette} avec statut {result['Statut']} non conforme, ignorée")
                continue

            cursor.execute("""
                UPDATE InfoPalette
                SET Statut = 'Renvoyé', Date_Modif_Statut = %s, Utilisateur_Modif_Statut = %s
                WHERE NumPalette = %s
            """, (now, utilisateur, data.num_palette))

            cursor.execute("""
                INSERT INTO STT_Palette (NumPalette, Statut, Date_Modif_Statut, Utilisateur_Modif_Statut)
                VALUES (%s, %s, %s, %s)
            """, (data.num_palette, "Renvoyé", now, utilisateur))

            #Trace mouvement
            cursor.execute("""
                INSERT INTO MVT_Palette (NumPalette, Date_Dernier_MVT, Zone)
                VALUES (%s, %s, %s)
            """, (data.num_palette, now, "Renvoyé"))

            #Liberation de l'emplacement
            cursor.execute("""
                UPDATE EmplacementEntrepot
                SET Etat = 'Libre'
                WHERE Emplacement = %s
            """, (emplacement,))

            valid_count += 1

        conn.commit()
        logging.info(f"{valid_count} palettes validées pour renvoie par {utilisateur}")
        return {"message": f"{valid_count} palettes validées pour renvoie"}

    except Exception as e:
        conn.rollback()
        logging.error(f"Erreur lors de la validation des renvois par {utilisateur} : {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Erreur lors de la validation")
    finally:
        conn.close()

# ----------------------------------------------------------

@router.get("/production")
def get_palettes_en_stock(user: User = Depends(get_current_user)):
    conn = get_connection()
    try:
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        cursor.execute("""
            SELECT NumPalette, Article, NomClient, Quantite, Emplacement
            FROM InfoPalette
            WHERE Statut = 'En Stock'
        """)
        results = cursor.fetchall()
        logging.info(f"Sortie production demandée par {getattr(user, 'username', user)}")
        return [sql_row_to_snake(row) for row in results]
    except Exception as e:
        logging.error(f"Erreur dans get_palettes_en_stock : {e}")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        conn.close()

# ----------------------------------------------------------

@router.post("/valider_production")
def valider_sortie_production(palettes: List[ValidationSortieProduction], user: User = Depends(get_current_user)):
    conn = get_connection()
    cursor = conn.cursor(pymysql.cursors.DictCursor)
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    utilisateur = getattr(user, "username", user.get("username", "inconnu"))
    
    try:
        conn.begin()
        valid_count = 0

        for data in palettes:
            # Vérifie l'existence et statut de la palette
            cursor.execute("SELECT Statut, Emplacement FROM InfoPalette WHERE NumPalette = %s", (data.num_palette,))
            result = cursor.fetchone()
            emplacement = result.get("Emplacement")
            if not result:
                logging.warning(f"Palette {data.num_palette} non trouvée, ignorée")
                continue
            if result["Statut"] != "En stock":
                logging.warning(f"Palette {data.num_palette} avec statut {result['Statut']} non conforme, ignorée")
                continue

            # Mise à jour statut
            cursor.execute("""
                UPDATE InfoPalette
                SET Statut = 'En Prod', Date_Modif_Statut = %s, Utilisateur_Modif_Statut = %s
                WHERE NumPalette = %s
            """, (now, utilisateur, data.num_palette))

            # Trace historique
            cursor.execute("""
                INSERT INTO STT_Palette (NumPalette, Statut, Date_Modif_Statut, Utilisateur_Modif_Statut)
                VALUES (%s, %s, %s, %s)
            """, (data.num_palette, "En Prod", now, utilisateur))

            #Trace mouvement
            cursor.execute("""
                INSERT INTO MVT_Palette (NumPalette, Date_Dernier_MVT, Zone)
                VALUES (%s, %s, %s)
            """, (data.num_palette, now, "Atelier"))

            #Liberation de l'emplacement
            cursor.execute("""
                UPDATE EmplacementEntrepot
                SET Etat = 'Libre'
                WHERE Emplacement = %s
            """, (emplacement,))

            valid_count += 1

        conn.commit()
        logging.info(f"{valid_count} palettes validées pour sortie production par {utilisateur}")
        return {"message": f"{valid_count} palettes validées pour sortie production"}

    except Exception as e:
        conn.rollback()
        logging.error(f"Erreur lors de la validation des sorties production par {utilisateur} : {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Erreur lors de la validation")
    finally:
        conn.close()