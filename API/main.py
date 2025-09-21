from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routes import auth, inventaire, consultation, entree, sorties, emplacement
import os
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(
    title="API Gestion de Palettes",
    description="API FastAPI pour gérer les entrées, mouvements et statuts de palettes",
    version="1.0",
    docs_url=None,
    redoc_url=None
)

origins = [
    "http://localhost",
    "http://127.0.0.1",
    "https://apimdo.fr"
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Inclusion des routes principales
app.include_router(auth.router, prefix="/auth", tags=["auth"])
app.include_router(inventaire.router, prefix="/inventaire", tags=["inventaire"])
app.include_router(consultation.router, prefix="/consultation", tags=["consultation"])
app.include_router(entree.router, prefix="/entree", tags=["entree"])
app.include_router(sorties.router, prefix="/sorties", tags=["sorties"])
app.include_router(emplacement.router, prefix="/emplacement", tags=["emplacement"])
