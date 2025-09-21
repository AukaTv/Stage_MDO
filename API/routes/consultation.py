from fastapi import APIRouter, HTTPException, Depends
from typing import Optional
from models.palette import EntreePalette, SortiePaletteComplete, ValidationInventaire
from models.user import User
from db import get_connection
from datetime import datetime
from auth import get_current_user
import logging
import html

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)

router = APIRouter()

# ----------------------------------------------------------

@router.get("")
def consulter_palettes(
    num_palette: Optional[str] = None,
    client: Optional[str] = None,
    article: Optional[str] = None,
    statut: Optional[str] = None,
    emplacement: Optional[str] = None,
    user: dict = Depends(get_current_user)
):
    conn = get_connection()
    try:
        conn.begin()
        cursor = conn.cursor()
        query = """
            SELECT NumPalette, NomClient, Article, Quantite, Emplacement, Date_Dernier_MVT, Statut, Date_Modif_Statut, Utilisateur_Modif_Statut
            FROM InfoPalette 
            WHERE 1=1
        """
        params = []

        if num_palette:
            query += " AND NumPalette LIKE %s"
            params.append(f"%{num_palette}%")
        if client:
            query += " AND NomClient LIKE %s"
            params.append(f"%{client}%")
        if article:
            query += " AND Article LIKE %s"
            params.append(f"%{article}%")
        if statut:
            query += " AND Statut = %s"
            params.append(statut)
        if emplacement:
            query += " AND Emplacement LIKE %s"
            params.append(f"%{emplacement}%")

        try:
            cursor.execute(query, params)
            results = cursor.fetchall()
            conn.commit()
        except Exception as e:
            conn.rollback()
            logging.error(f"Erreur lors de l'exécution de la requête SQL: {e}")
            raise HTTPException(status_code=500, detail="Erreur interne du serveur")

        return [
            {
                "num_palette": html.escape(row["NumPalette"]),
                "nom_client": html.escape(row["NomClient"]),
                "article": html.escape(row["Article"]),
                "quantite": row["Quantite"],
                "emplacement": html.escape(row["Emplacement"]) if row["Emplacement"] else None,
                "date_dernier_mvt": str(row["Date_Dernier_MVT"]) if row["Date_Dernier_MVT"] else None,
                "statut": html.escape(row["Statut"]),
                "date_modif_statut": str(row["Date_Modif_Statut"]) if row["Date_Modif_Statut"] else None,
                "utilisateur_modif_statut": html.escape(row["Utilisateur_Modif_Statut"])
            }
            for row in results
        ]
    finally:
        conn.close()