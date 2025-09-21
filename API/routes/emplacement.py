from typing import Optional
from fastapi import APIRouter, HTTPException
from models.palette import EntreePalette, SortiePaletteComplete, ValidationInventaire
from models.user import User
from db import get_connection
from datetime import datetime
from fastapi import Depends
from auth import get_current_user
from pydantic import BaseModel
from utils.helpers import sql_row_to_snake

router = APIRouter()

# ----------------------------------------------------------

class MiseAJourEmplacement(BaseModel):
    num_palette: str
    nouvel_emplacement: str

@router.patch("/palette")
def mettre_a_jour_emplacement(data: MiseAJourEmplacement, user: dict = Depends(get_current_user)):
    data.num_palette = data.num_palette.strip()
    data.nouvel_emplacement = data.nouvel_emplacement.strip()

    conn = get_connection()
    cursor = conn.cursor()
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    try:
        conn.begin()

        # Vérifie si la palette existe
        cursor.execute("SELECT COUNT(*) AS count FROM InfoPalette WHERE NumPalette = %s", (data.num_palette,))
        if cursor.fetchone()["count"] == 0:
            raise HTTPException(status_code=404, detail="Palette non trouvée")

        # Mise à jour de l'emplacement dans InfoPalette
        cursor.execute("""
            UPDATE InfoPalette
            SET Emplacement = %s, Date_Modif_Statut = %s, Utilisateur_Modif_Statut = %s
            WHERE NumPalette = %s
        """, (data.nouvel_emplacement, now, user["username"], data.num_palette))

        # Insertion dans Mvt_Palette
        cursor.execute("""
            INSERT INTO Mvt_Palette (NumPalette, Date_Dernier_MVT, Zone)
            VALUES (%s, %s, %s)
        """, (data.num_palette, now, data.nouvel_emplacement))

        conn.commit()
        return {"message": "Emplacement mis à jour avec succès"}

    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=500, detail=f"Erreur interne: {str(e)}")

    finally:
        conn.close()


# ----------------------------------------------------------
class EmplacementEntrepotResponse(BaseModel):
    emplacement: str
    etat: str

@router.get("", response_model=list[EmplacementEntrepotResponse])
def get_emplacements(user: dict = Depends(get_current_user)):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT Emplacement, Etat FROM EmplacementEntrepot")
    emplacements = cursor.fetchall()
    conn.close()
    # Tu peux return direct : la conversion SQL → dict est gérée par FastAPI avec pymysql/cursor.DictCursor.
    return [{"emplacement": row["Emplacement"], "etat": row["Etat"]} for row in emplacements]