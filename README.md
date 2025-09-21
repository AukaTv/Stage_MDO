# Application MDO – Suite de gestion de palettes

> **Desktop (Poste fixe & Consultation) + API FastAPI + Client Android**

Ce dépôt contient une suite applicative destinée à la gestion opérationnelle de palettes (création de fiches, consultation, inventaire, mouvements), intégrant des environnements hétérogènes (Windows/Access, Linux/MySQL) avec impression industrielle (PDF + codes‑barres).

---

## 📦 Composants

* **API/** – Backend **FastAPI** (inventaire, consultation, entrées/sorties, emplacements, auth)
* **poste\_fixe/** – Application **Tkinter** (poste industriel) : saisie manuelle/automatique, génération de PDF, impression, insertion MySQL
* **consultation/** – Application **PyQt5** : authentification, vues de consultation/filtrage/export CSV
* **Dynamic DRB/** – Client **Android (Java)** interfacé avec l’API (activities/adapters/models)

```
Application_MDO-main/
├─ API/
│  ├─ main.py
│  ├─ Dockerfile             #Sert à la création de l'image docker de l'API
│  ├─ requirements.txt       #Sert à importer les dépendances python
│  ├─ auth.py                #Sert à gérer l'authentification des routes avec password et login délivrant un token
│  ├─ db.py                  #Sert à gérer la connexion à la base de donnée mysql
│  └─ routes/ (auth, inventaire, consultation, entree, sorties, emplacement)
│  └─ models/ (palette, user)
│  └─ utils/ (helpers)
├─ poste_fixe/
│  ├─ auto_compil.bat         #Sert à automatiser la compilation du .py en .exe avec PyInstaller
│  ├─ Main.py
│  └─ Data/
│     ├─ .env                 # variables d’environnement (non versionnées en prod)
│     └─ types_palettes.json  # cartographie type→poids
├─ consultation/
│  ├─ auto_compil.bat         #Sert à automatiser la compilation du .py en .exe avec PyInstaller
│  ├─ Main.py
│  └─ Data/
│     └─ .env
└─ Dynamic DRB/ (Android)
```

---

## 🚀 Fonctionnalités clés

* **Poste fixe (Tkinter)**

  * Mode **Auto** (connexion Access via ODBC : récupère les informations clients et opérations depuis une base déjà existante) ou **Manuel** (saisie libre)
  * **Génération de fiches palette** PDF (ReportLab) en A4 paysage
  * **Codes‑barres Code128** (référence, quantité, employé, numéro de palette)
  * **Impression silencieuse** via Adobe Reader (`/t`)
  * **Insertion MySQL** (table `InfoPalette`) avec horodatage et calcul automatique du **NumPalette** (11 chiffres) et de la quantité

* **Consultation (PyQt5)**

  * Écran de **connexion** (bcrypt) et accès à la base MySQL
  * **Filtrage**, tri, rafraîchissement périodique, **export CSV**

* **API (FastAPI)**

  * Endpoints **REST**: `auth`, `inventaire`, `consultation`, `entree`, `sorties`, `emplacement`
  * CORS configurable, structure modulaire par routeur

* **Sécurité & Config**

  * Secrets chargés via **`.env`**
  * **Chiffrement Fernet** pour `ACCESS_PASSWORD` / `MYSQL_PASSWORD`
  * Séparation **legacy** (Access) / **production** (MySQL)

---

## 🧩 Prérequis

### Commun

* Python 3.10+
* `pip install -r requirements.txt` *(si présent – sinon voir listes ci‑dessous)*

### Poste fixe

* Windows (pour driver Access + Adobe Reader)
* **Microsoft Access Database Engine** (x86/x64 selon Python)
* **Adobe Acrobat Reader** installé (chemin référencé dans `.env`)
* Python packages : `pyodbc`, `pymysql`, `python-dotenv`, `cryptography`, `reportlab`, `tkinter` (inclus CPython Windows)

### Consultation (Windows/Linux)

* Python packages : `PyQt5`, `pymysql`, `python-dotenv`, `bcrypt`, `logging`

### API (Linux/Windows)

* Python packages : `fastapi`, `uvicorn`, `python-dotenv`
* Base **MySQL** accessible

---

## 🔐 Variables d’environnement

> **Ne pas versionner** vos `.env` en production. Utiliser plutôt un `.env.example`.

### `poste_fixe/Data/.env`

```env
# Access (legacy)
MDB_PATH=
MDW_PATH=
ACCESS_USER=
ACCESS_PASSWORD=  # mot de passe CHIFFRÉ via Fernet

# MySQL (production)
MYSQL_HOST=
MYSQL_PORT=3306
MYSQL_BNAME=
MYSQL_USER=
MYSQL_PASSWORD=    # mot de passe CHIFFRÉ via Fernet

# Impression (Adobe Reader)
AAR_PATH=          # ex: C:\Program Files (x86)\Adobe\Acrobat Reader DC\Reader\AcroRd32.exe

# Clé de chiffrement
FERNET_KEY=        # base64 urlsafe (44 chars typ.)
```

### `consultation/Data/.env`

```env
MYSQL_HOST=
MYSQL_PORT=3306
MYSQL_BNAME=
MYSQL_USER=
MYSQL_PASSWORD=    # clair ou chiffré selon implémentation de Consultation
```

### API/.env

```env
# Exemple minimal
DB_HOST=
DB_PORT=3306
DB_NAME=
DB_USER=
DB_PASSWORD=
ORIGINS=*          # CORS (à restreindre en prod)
```

---

## 🔒 Chiffrement Fernet – utilitaire

Pour chiffrer un mot de passe avec **la même `FERNET_KEY`** que celle utilisée par `poste_fixe` :

```python
from cryptography.fernet import Fernet
key = b"VOTRE_FERNET_KEY=="  # même clé que dans .env
cipher = Fernet(key)
print(cipher.encrypt(b"motdepasse_en_clair").decode())
```

Collez la valeur encodée Base64 dans `ACCESS_PASSWORD` / `MYSQL_PASSWORD` de votre `.env`.

---

## 🗃️ Base de données (MySQL / Access)

* **Access** : legacy, utilisé en mode Auto
* **MySQL (DRB)** : base principale (InfoPalette, SPR\_Palette, STT\_Palette, MVT\_Palette, EmplacementEntrepot, Users)

### Script d’initialisation MySQL (extrait)

```sql
CREATE DATABASE IF NOT EXISTS DRB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE DRB;

CREATE TABLE IF NOT EXISTS InfoPalette (...);
CREATE TABLE IF NOT EXISTS SPR_Palette (...);
CREATE TABLE IF NOT EXISTS STT_Palette (...);
CREATE TABLE IF NOT EXISTS MVT_Palette (...);
CREATE TABLE IF NOT EXISTS EmplacementEntrepot (...);
CREATE TABLE IF NOT EXISTS Users (...);

-- Insertion de comptes (mots de passe bcrypt)
INSERT INTO Users (Login, Password, Role) VALUES
('admin', '...hash...', 'admin'),
('Consultation', '...hash...', 'utilisateur');
```
---
