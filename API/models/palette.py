from pydantic import BaseModel
from typing import Optional

class EntreePalette(BaseModel):
    num_palette: str
    emplacement: Optional[str] = None
    motif: Optional[str] = None

class SortiePaletteComplete(BaseModel):
    num_palette: str
    motif: str
    statut: str

class ValidationInventaire(BaseModel):
    num_palette: str
    quantite: Optional[int] = None
    statut: str
    emplacement: str

class EmplacementEntrepotResponse(BaseModel):
    emplacement: str
    etat: str

class ValidationDestruction(BaseModel):
    num_palette: str

class ValidationRenvoie(BaseModel):
    num_palette: str

class ValidationSortieProduction(BaseModel):
    num_palette: str
    quantite: Optional[int] = None
    statut: str