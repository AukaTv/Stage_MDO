import os
import jwt
from jwt import PyJWTError, ExpiredSignatureError
from datetime import datetime, timedelta
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from dotenv import load_dotenv
from models.user import User

load_dotenv()

SECRET_KEY = os.getenv("JWT_SECRET")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60

API_USER = os.getenv("API_USER")
API_PASSWORD = os.getenv("API_PASSWORD")

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")

def authenticate_user(username: str, password: str):
    if username == API_USER and password == API_PASSWORD:
        return User(username=username)
    return None

def create_access_token(data: dict, expires_delta: timedelta = None):
    to_encode = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    to_encode.update({"iss": "apimdo-backend"})
    to_encode.update({"sub": data.get("username")})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

def get_current_user(token: str = Depends(oauth2_scheme)):
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        if payload.get("iss") != "apimdo-backend":
            raise HTTPException(status_code=401, detail="Accès non autorisé.")
        username = payload.get("sub")
        if username != API_USER:
            raise HTTPException(status_code=401, detail="Accès non autorisé.")
        return {"username": username}
    except ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Accès non autorisé.",
            headers={"WWW-Authenticate": "Bearer"}
        )
    except PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Accès non autorisé.",
            headers={"WWW-Authenticate": "Bearer"}
        )
