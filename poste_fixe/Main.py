
import os
import json
import pyodbc
import tkinter as tk
from tkinter import ttk
from tkinter import messagebox
from datetime import datetime
from dotenv import load_dotenv
from reportlab.lib.pagesizes import landscape, A4
from reportlab.pdfgen import canvas
from reportlab.graphics.barcode import code128
from reportlab.lib.units import mm
from reportlab.lib.utils import ImageReader
from cryptography.fernet import Fernet
import subprocess
import pymysql
import sys

# Redirige tous les prints et erreurs Python dans debug_postefixe.log (et la console)
import sys
class TeeLogger:
    def __init__(self, *files):
        self.files = [f for f in files if f is not None]
    def write(self, obj):
        for f in self.files:
            try:
                f.write(obj)
                f.flush()
            except Exception:
                pass
    def flush(self):
        for f in self.files:
            try:
                f.flush()
            except Exception:
                pass

logfile = open("error_log.log", "a", encoding="utf-8")
sys.stdout = TeeLogger(getattr(sys, "__stdout__", None), logfile)
sys.stderr = TeeLogger(getattr(sys, "__stderr__", None), logfile)


# Fonction qui intercepte les exceptions non gérées et les enregistre dans un fichier texte
#Utile pour debugger quand l'application compilée plante silencieusement
def log_uncaught_exceptions(exctype, value, tb):
    with open("error_log.txt", "a", encoding="utf-8") as f:
        import traceback
        f.write("=== Exception non interceptée ===\n")
        traceback.print_exception(exctype, value, tb, file=f)

sys.excepthook = log_uncaught_exceptions

# Fonction utilitaire pour accéder aux fichiers dans le dossier 'data'
def resource_path(filename):
    return os.path.join(os.path.dirname(sys.executable if getattr(sys, 'frozen', False) else __file__), "data", filename)

#====================================================================================================================

# Chargement des variables d'environnement
load_dotenv(resource_path(".env"))

#Cryptage
fernet = Fernet(os.getenv("FERNET_KEY").encode())

#Access
mdb_path = os.getenv("MDB_PATH")
mdw_path = os.getenv("MDW_PATH")
user = os.getenv("ACCESS_USER")
password = fernet.decrypt(os.getenv("ACCESS_PASSWORD").encode()).decode()
#MySQL
mysql_host = os.getenv("MYSQL_HOST")
mysql_port = int(os.getenv("MYSQL_PORT", "3306"))
mysql_bname = os.getenv("MYSQL_BNAME")
mysql_user = os.getenv("MYSQL_USER")
mysql_password = fernet.decrypt(os.getenv("MYSQL_PASSWORD").encode()).decode()
#Adobe Path
aar_path = os.getenv("AAR_PATH")

# Connexion au base de données
#Connexion Access
conn_str = (
    r"DRIVER={Microsoft Access Driver (*.mdb)};"
    rf"DBQ={mdb_path};"
    rf"SystemDB={mdw_path};"
    rf"UID={user};"
    rf"PWD={password};"
)
conn = pyodbc.connect(conn_str)
cursor = conn.cursor()

# Fonction utilitaire pour obtenir une connexion MySQL (à utiliser pour chaque opération critique)
def get_mysql_conn():
    return pymysql.connect(
        host=mysql_host,
        port=mysql_port,
        user=mysql_user,
        password=mysql_password,
        database=mysql_bname,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor
    )

#====================================================================================================================
# Chargement des types de palettes dans le json, possibilité de rajouter en accord avec le formatage en place
with open(resource_path("types_palettes.json"), "r", encoding="utf-8") as f:
    types_palettes = json.load(f)

# Fonction qui récupère le prochain numéro de palette depuis la base MySQL
# Ne prend que les NumPalette composés exclusivement de 11 chiffres
def get_next_palette_number():
    try:
        with get_mysql_conn() as conn:
            with conn.cursor() as cursor:
                # Ne prend que les NumPalette composés exclusivement de 11 chiffres
                cursor.execute("SELECT MAX(NumPalette) AS Num FROM InfoPalette WHERE NumPalette REGEXP '^[0-9]{11}$';")
                result = cursor.fetchone()
                last_num = int(result['Num']) if result['Num'] else 90000000000
                return str(last_num + 1)
    except Exception as e:
        print(f"Erreur MySQL : {e}")
        return str(90000000001)

# Fonction qui retourne le poste (Matin, Après-midi ou Nuit) en fonction de l'heure actuelle
#Sert à horodater l'entrée dans la base de données
def get_poste():
    heure = datetime.now().hour
    if 5 <= heure < 13:
        return "Matin"
    elif 13 <= heure < 21:
        return "Après-midi"
    else:
        return "Nuit"

#Fonction pour autofocus tout le champ
def select_all(event):
    event.widget.select_range(0, 'end')
    event.widget.icursor('end')
    return 'break'

# Fonction qui génère le fichier PDF de la fiche palette et l'insère dans la base MySQL
#Utilise les données de GDC passées sous forme de dictionnaire 'data'
def create_fiche_pdf(data, type_document, output_path="fiche_palette.pdf"):
    c = canvas.Canvas(output_path, pagesize=landscape(A4))
    width, height = landscape(A4)

    ligne_y = height - 90
    esp = 25

    # Nom du client
    ligne_y -= esp
    c.setFont("Helvetica-Bold", 120)
    c.drawString(40, ligne_y, data['NomClient'])
    ligne_y -= esp * 3

    # Référence
    c.setFont("Helvetica-Bold", 55)
    c.drawString(40, ligne_y, data['Article'])
    ligne_y -= esp * 1

     # Code-barres quantité
    c.setFont("Helvetica-Bold", 25)
    ligne_y -= esp*1.5
    c.drawString(60, ligne_y, "Code-barres Quantité :")
    ligne_y -= esp*1.5
    barcode_qte = code128.Code128(str(data['Quantite']), barHeight=20 * mm, barWidth=3)
    barcode_qte.drawOn(c, 400, ligne_y)

    # Quantité
    ligne_y -= esp*5
    c.setFont("Helvetica-Bold", 130)
    if type_document == "Film":
        c.drawString(40, ligne_y, f"{data['Quantite']:,}".replace(",", " ") + ' KG')
    if type_document == "Document":
        c.drawString(40, ligne_y, f"{data['Quantite']:,}".replace(",", " ") + ' EX')
    if type_document == "Carton":
        c.drawString(40, ligne_y, f"{data['Quantite']:,}".replace(",", " ") + ' EX')
    if type_document == "Livraison":
        c.drawString(40, ligne_y, f"{data['Quantite']:,}".replace(",", " ") + ' EX')

    ligne_y -= esp * 2
    c.setFont("Helvetica-Bold", 40)
    c.drawString(40, ligne_y, str(data['Horodatage'])+ ' ' + str(data['NomEmploye']))
    ligne_y -= esp * 1

    # Code-barres NumPalette
    c.setFont("Helvetica-Bold", 25)
    ligne_y -= esp*2
    c.drawString(60, ligne_y, "Numéro de palette :")
    c.drawString(60, ligne_y - esp, f"{data['NumPalette']}")
    ligne_y -= esp*1.5
    barcode_palette = code128.Code128(data['NumPalette'], barHeight=20 * mm, barWidth=3)
    barcode_palette.drawOn(c, 400, ligne_y)

    # Finaliser
    c.showPage()
    c.save()

    # Insertion dans la base MySQL
    try:
        with get_mysql_conn() as conn:
            with conn.cursor() as cursor:
                sql = """
                    INSERT INTO InfoPalette (
                        NumPalette,
                        NumOperation,
                        Operation,
                        Article,
                        NomClient,
                        ActionClient,
                        Quantite,
                        Employe,
                        Date_Entree_Reliquat
                    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                """
                cursor.execute(sql, (
                    data["NumPalette"],
                    data["NumOperation"],
                    data["Operation"],
                    data["Article"],
                    data["NomClient"],
                    data["ActionClient"],
                    data["Quantite"],
                    data["NomEmploye"],
                    data["MySQL_Horodatage"]
                ))
            conn.commit()
    except Exception as e:
        print(f"Erreur MySQL : {e}")
        messagebox.showerror("Erreur MySQL", f"Insertion impossible dans la base MySQL.\n\nDétail : {e}")


# Fonction qui lance l'impression du PDF via Adobe Acrobat Reader
#Le chemin est chargé dynamiquement depuis une variable d'environnement
def imprimer_pdf(pdf_path):
    acrobat_path = aar_path
    if not os.path.exists(acrobat_path):
        print("Acrobat Reader introuvable. Vérifie le chemin.")
        return

    try:
        subprocess.Popen([
            acrobat_path,
            "/t",
            pdf_path
        ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        print("Impression lancée")
    except Exception as e:
        print(f"Erreur lors de l'impression : {e}")

# Fonction gérant la enêtre principale permettant à l'utilisateur de choisir le mode de fonctionnement
#Lance le mode automatique (connexion à Access) ou manuel (saisie libre)
def start_mode_selection():
    mode_root = tk.Tk()
    mode_root.title("Sélection du mode")
    mode_root.geometry("400x300")

    try:
        global conn, cursor
        conn = pyodbc.connect(conn_str)
        print("Connexion Access réussie.")
        cursor = conn.cursor()
    except Exception as e:
        print(f"Échec connexion Access : {e}")
        messagebox.showerror("Erreur", f"Connexion Access impossible :\n{e}")
        mode_root.destroy()
        return
    
    # Fonction gérant la fenêtre principale permettant à l'utilisateur de choisir le mode de fonctionnement  
    #Lance le mode automatique (connexion à Access) ou manuel (saisie libre)
    def lancer_mode(mode):
        mode_root.destroy()
        lancer_app(mode)

    tk.Label(mode_root, text="Choisissez un mode de fonctionnement :", font=("Arial", 14)).pack(pady=20)
    tk.Button(mode_root, text="Mode Automatique (Scan Opération)", font=("Arial", 12), command=lambda: lancer_mode("auto")).pack(pady=10)
    tk.Button(mode_root, text="Mode Manuel (Entrée manuelle)", font=("Arial", 12), command=lambda: lancer_mode("manuel")).pack(pady=10)
    tk.Button(mode_root, text="Paramètres", font=("Arial", 12), command=ouvrir_parametres_gui).pack(pady=10)

    mode_root.mainloop()

# Fonction qui ouvre une fenêtre de paramètres pour modifier le chemin d'Adobe Acrobat Reader et aussi d'ajouter un type de palette
def ouvrir_parametres_gui():
    param_win = tk.Toplevel()
    param_win.title("Paramètres")
    param_win.geometry("600x400")

    tk.Label(param_win, text="Chemin Adobe Acrobat Reader :", font=("Arial", 12)).pack(pady=10)
    aar_var = tk.StringVar(value=os.getenv("AAR_PATH", ""))

    tk.Entry(param_win, textvariable=aar_var, width=80, font=("Arial", 12)).pack()

    def enregistrer():
        new_path = aar_var.get().strip()
        updated = False
        env_path = resource_path(".env")
        print("[DEBUG] Chemin .env utilisé :", env_path)

        with open(env_path, "r", encoding="utf-8") as f:
            lines = f.readlines()

        with open(env_path, "w", encoding="utf-8") as f:
            for line in lines:
                if line.strip().startswith("AAR_PATH ="):
                    f.write(f"AAR_PATH ={new_path}\n")
                    updated = True
                else:
                    f.write(line)
            if not updated:
                f.write(f"AAR_PATH ={new_path}\n")

        load_dotenv(env_path, override=True)
        global aar_path
        aar_path = os.getenv("AAR_PATH")
        messagebox.showinfo("Succès", f"Chemin AAR_PATH mis à jour")
        param_win.destroy()

    tk.Label(param_win, text="Ajouter un type de palette :", font=("Arial", 12)).pack(pady=(20, 5))

    frame_palette = tk.Frame(param_win)
    frame_palette.pack(pady=5)

    tk.Label(frame_palette, text="Nom :", font=("Arial", 10)).grid(row=0, column=0, padx=5)
    new_palette_name = tk.Entry(frame_palette, font=("Arial", 10))
    new_palette_name.grid(row=0, column=1, padx=5)

    tk.Label(frame_palette, text="Poids (kg) :", font=("Arial", 10)).grid(row=0, column=2, padx=5)
    new_palette_weight = tk.Entry(frame_palette, font=("Arial", 10))
    new_palette_weight.grid(row=0, column=3, padx=5)

    def ajouter_palette():
        name = new_palette_name.get().strip()
        try:
            weight = float(new_palette_weight.get().strip())
        except ValueError:
            messagebox.showerror("Erreur", "Poids invalide (doit être un nombre).")
            return

        if not name:
            messagebox.showerror("Erreur", "Le nom ne peut pas être vide.")
            return

        json_path = resource_path("types_palettes.json")
        with open(json_path, "r", encoding="utf-8") as f:
            palettes = json.load(f)

        palettes[name] = weight

        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(palettes, f, indent=2, ensure_ascii=False)

        global types_palettes
        types_palettes = palettes
        try:
            type_palette_combo['values'] = list(types_palettes.keys())
        except:
            pass

        messagebox.showinfo("Succès", f"Type de palette '{name}' ajouté avec {weight} kg.")
        new_palette_name.delete(0, tk.END)
        new_palette_weight.delete(0, tk.END)

    tk.Button(param_win, text="Ajouter type de palette", command=ajouter_palette, bg="#2196F3", fg="white").pack(pady=10)

    tk.Button(param_win, text="Enregistrer", command=enregistrer, bg="#4CAF50", fg="white").pack(pady=10)
    try:
        with open(BACKUP_FILE, "r", encoding="utf-8") as f:
            backup_var.set(f.read().strip())
    except:
        backup_var.set(str(DEFAULT_BACKUP_START))

    backup_entry = tk.Entry(param_win, textvariable=backup_var, font=("Arial", 12), width=30)
    backup_entry.pack(pady=5)

    def enregistrer_backup():
        try:
            new_value = int(backup_var.get().strip())
        except ValueError:
            messagebox.showerror("Erreur", "Valeur invalide (doit être un entier).")
            return

        with open(BACKUP_FILE, "w", encoding="utf-8") as f:
            f.write(str(new_value))

        global DEFAULT_BACKUP_START
        DEFAULT_BACKUP_START = new_value
        messagebox.showinfo("Succès", f"Valeur de secours mise à jour : {new_value}")

    tk.Button(param_win, text="Mettre à jour numéro de secours", command=enregistrer_backup, bg="#FF9800", fg="white").pack(pady=10)

# Fonction appelée lors d’un retour au menu principal depuis l’interface
#Ferme la fenêtre active et relance la sélection du mode
def retour_menu(fenetre):
    fenetre.destroy()
    start_mode_selection()

# Fonction principale qui affiche l'interface graphique selon le mode choisi
#Mode "auto" : récupère les infos depuis Access / Mode "manuel" : saisie manuelle
def filter_employe_options(event, combo, all_options):
    current_text = combo.get().lower()
    filtered = [name for name in all_options if name.lower().startswith(current_text)]
    combo['values'] = filtered

def lancer_app(mode):
    root = tk.Tk()
    root.title("Poste Fixe - Fiche Palette")
    root.geometry("1440x900")

    # Variables
    num_operation_var = tk.StringVar()
    article_var = tk.StringVar()
    poids_palette_var = tk.DoubleVar()
    poids_10_ex_var = tk.DoubleVar()
    type_palette_var = tk.StringVar()
    type_document_var = tk.StringVar()
    nb_cartons_var = tk.DoubleVar()
    nb_docs_par_carton_var = tk.IntVar()
    employe_var = tk.StringVar()
    quantite_directe_var = tk.IntVar()
    info_vars = {}
    articles = {}
    employes = {}

    # Chargement des Employés
    if mode == "auto":
        try:
            cursor.execute("SELECT IdEmploye, NomEmploye FROM T_Employes ORDER BY NomEmploye ASC")
            employe_rows = cursor.fetchall()
            for row in employe_rows:
                employes[row.NomEmploye] = row.IdEmploye
        except Exception as e:
            print(f"Erreur chargement employés : {e}")
            messagebox.showerror("Erreur Access", f"Impossible de charger les employés :\n{e}")
            root.destroy()
            return

        # Fonction de chargement d'une opération à partir du numéro saisi (mode automatique uniquement)
        #Récupère les données liées à l'opération depuis la base Access et met à jour l'interface
        def charger_operation(event=None):
            num_op = num_operation_var.get().strip().strip("*")
            if not num_op.isdigit():
                client_label.config(text="Entrée invalide")
                return

            try:
                cursor.execute("""
                    SELECT
                        n.NumOperation,
                        o.Operation,
                        n.Article,
                        a.Designation,
                        c.NomClient,
                        ac.ActionClient
                    FROM (((nomenclatures AS n
                    INNER JOIN articles AS a ON n.Article = a.Article)
                    INNER JOIN opération AS o ON n.NumOperation = o.NumOperation)
                    INNER JOIN actions AS ac ON o.NumAction = ac.NumAction)
                    INNER JOIN clients AS c ON ac.NumClient = c.CodeClient
                    WHERE n.NumOperation = ?;
                """, num_op)
                rows = cursor.fetchall()
            except Exception as e:
                print(f"Erreur requête opération : {e}")
                messagebox.showerror("Erreur Access", f"Erreur lors du chargement de l'opération :\n{e}")
                return

            if not rows:
                client_label.config(text="Opération introuvable")
                return

            info_vars["NumOperation"] = rows[0].NumOperation
            info_vars["Operation"] = rows[0].Operation
            info_vars["NomClient"] = rows[0].NomClient
            info_vars["ActionClient"] = rows[0].ActionClient

            articles.clear()
            for row in rows:
                articles[row.Article] = row.Designation
            article_combo['values'] = list(articles.keys())
            article_combo.current(0)

            client_label.config(text=f"Client : {rows[0].NomClient}")
            action_label.config(text=f"Action : {rows[0].ActionClient}")
            num_operation_var.set("")

    # Fonction appelée lors du clic sur "Imprimer"
    #Elle récupère les données de l’interface, calcule la quantité, génère un PDF et l’imprime
    def generer_et_imprimer():
        print("[DEBUG] Fonction generer_et_imprimer appelée")
        ref = article_var.get()
        designation = articles.get(ref, "") if mode == "auto" else designation_var.get()
        poids_palette = poids_palette_var.get()
        poids_vide = types_palettes.get(type_palette_var.get(), 0)
        poids_net = poids_palette - poids_vide
        
        # Calcul de la quantité selon le type de document
        if type_document_var.get() == "Document":
            quantite = int(round((poids_net * 10000) / poids_10_ex_var.get())) if poids_10_ex_var.get() > 0 else 0
        elif type_document_var.get() == "Film":
            quantite = int(round(poids_net)) if poids_net > 0 else 0
        elif type_document_var.get() == "Carton":
            quantite = int(nb_cartons_var.get() * nb_docs_par_carton_var.get())
        elif type_document_var.get() == "Livraison":
            quantite = quantite_directe_var.get()
        else:
            quantite = 0

        data = {
            "NumOperation": info_vars["NumOperation"] if mode == "auto" else num_operation_var.get(),
            "Operation": info_vars["Operation"] if mode == "auto" else operation_var.get(),
            "NomClient": info_vars["NomClient"] if mode == "auto" else client_var.get(),
            "ActionClient": info_vars["ActionClient"] if mode == "auto" else action_var.get(),
            "Article": ref,
            "Designation": designation,
            "Quantite": quantite,
            "NumPalette": get_next_palette_number(),
            "Poste": get_poste(),
            "Horodatage": datetime.now().strftime("%d/%m/%Y %H:%M"),
            "MySQL_Horodatage": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "NomEmploye": employe_var.get()
        }

        create_fiche_pdf(data, type_document_var.get())
        print("[DEBUG] PDF généré, tentative d'impression")
        imprimer_pdf("fiche_palette.pdf")
        print("[DEBUG] Impression terminée")

        # Réinitialisation des champs
        num_operation_var.set("")
        article_var.set("")
        poids_palette_var.set(0.0)
        poids_10_ex_var.set(0.0)
        nb_cartons_var.set(0)
        nb_docs_par_carton_var.set(0)
        type_palette_var.set("")
        type_document_var.set("")
        employe_var.set("")
        if mode == "manuel":
            operation_var.set("")
            client_var.set("")
            action_var.set("")
            designation_var.set("")

        quantite_directe_var.set(0)

    # Interface de l'application
    font_label = ("Arial", 16)
    font_entry = ("Arial", 16)

    # Widgets spécifiques à Document/Carton
    poids_10_label = tk.Label(root, text="Poids de 10 exemplaires (g) :", font=font_label)
    poids_10_entry = tk.Entry(root, textvariable=poids_10_ex_var, font=font_entry)
    nb_cartons_label = tk.Label(root, text="Nombre de cartons :", font=font_label)
    nb_cartons_entry = tk.Entry(root, textvariable=nb_cartons_var, font=font_entry)
    nb_docs_label = tk.Label(root, text="Documents par carton :", font=font_label)
    nb_docs_entry = tk.Entry(root, textvariable=nb_docs_par_carton_var, font=font_entry)

    # Fonction appelée lorsqu'on change le type de document (Document, Film, Carton)
    #Elle met à jour dynamiquement les champs affichés dans l'interface en fonction du type sélectionné
    def update_fields(*args):

        poids_10_label.grid_remove()
        poids_10_entry.grid_remove()
        nb_cartons_label.grid_remove()
        nb_cartons_entry.grid_remove()
        nb_docs_label.grid_remove()
        nb_docs_entry.grid_remove()
        quantite_label.grid_remove()
        quantite_entry.grid_remove()

        if mode == "auto":
            type_palette_label.grid_remove()
            type_palette_combo.grid_remove()
        else:
            type_palette_label.grid_remove()
            type_palette_combo.grid_remove()

        if 'poids_palette_label' in globals() or 'poids_palette_label' in locals():
            try:
                poids_palette_label.grid_remove()
                poids_palette_entry.grid_remove()
            except Exception:
                pass
        quantite_label.grid_remove()
        quantite_entry.grid_remove()
        if type_document_var.get() == "Document":
            poids_10_label.grid(row=poids_10_row, column=0, sticky="e", padx=5, pady=5)
            poids_10_entry.grid(row=poids_10_row, column=1, columnspan=2, sticky="we", padx=5, pady=5)
            poids_10_entry.bind("<FocusIn>", select_all)

            type_palette_label.grid(row=type_palette_row, column=0, sticky="e", padx=5, pady=5)
            type_palette_combo.grid(row=type_palette_row, column=1, columnspan=2, sticky="we", padx=5, pady=5)

            if mode == "auto":
                poids_palette_label.grid(row=9, column=0, sticky="e", padx=5, pady=5)
                poids_palette_entry.grid(row=9, column=1, columnspan=2, sticky="we", padx=5, pady=5)
                poids_palette_entry.bind("<FocusIn>", select_all)
            else:
                poids_palette_label.grid(row=11, column=0, sticky="e", padx=5, pady=5)
                poids_palette_entry.grid(row=11, column=1, columnspan=2, sticky="we", padx=5, pady=5)
                poids_palette_entry.bind("<FocusIn>", select_all)
        elif type_document_var.get() == "Carton":
            nb_cartons_label.grid(row=nb_cartons_row, column=0, sticky="e", padx=5, pady=5)
            nb_cartons_entry.grid(row=nb_cartons_row, column=1, columnspan=2, sticky="we", padx=5, pady=5)
            nb_cartons_entry.bind("<FocusIn>", select_all)
            nb_docs_label.grid(row=nb_docs_row, column=0, sticky="e", padx=5, pady=5)
            nb_docs_entry.grid(row=nb_docs_row, column=1, columnspan=2, sticky="we", padx=5, pady=5)
            nb_docs_entry.bind("<FocusIn>", select_all)

            try:
                poids_palette_label.grid_remove()
                poids_palette_entry.grid_remove()
            except Exception:
                pass

        elif type_document_var.get() == "Film":

            type_palette_label.grid(row=type_palette_row, column=0, sticky="e", padx=5, pady=5)
            type_palette_combo.grid(row=type_palette_row, column=1, columnspan=2, sticky="we", padx=5, pady=5)

            if mode == "auto":
                poids_palette_label.grid(row=9, column=0, sticky="e", padx=5, pady=5)
                poids_palette_entry.grid(row=9, column=1, columnspan=2, sticky="we", padx=5, pady=5)
                poids_palette_entry.bind("<FocusIn>", select_all)
            else:
                poids_palette_label.grid(row=11, column=0, sticky="e", padx=5, pady=5)
                poids_palette_entry.grid(row=11, column=1, columnspan=2, sticky="we", padx=5, pady=5)
                poids_palette_entry.bind("<FocusIn>", select_all)
        elif type_document_var.get() == "Livraison":
            quantite_label.grid(row=10, column=0, sticky="e", padx=5, pady=5)
            quantite_entry.grid(row=10, column=1, columnspan=2, sticky="we", padx=5, pady=5)
            quantite_entry.bind("<FocusIn>", select_all)

    # Mode automatique si connexion possible à GDC
    if mode == "auto":
        tk.Label(root, text="Numéro d'opération :", font=font_label).grid(row=0, column=0, sticky="e", padx=5, pady=5)
        entry = tk.Entry(root, textvariable=num_operation_var, font=font_entry)
        entry.grid(row=0, column=1, padx=5, pady=5, sticky="we")
        entry.bind("<Return>", charger_operation)
        entry.focus_set()
        tk.Button(root, text="Charger", font=font_entry, command=charger_operation).grid(row=0, column=2, padx=5, pady=5)

        client_label = tk.Label(root, text="Client : ", font=font_label)
        client_label.grid(row=1, column=0, columnspan=3, sticky="w", padx=10)

        action_label = tk.Label(root, text="Action : ", font=font_label)
        action_label.grid(row=2, column=0, columnspan=3, sticky="w", padx=10)

        tk.Label(root, text="Article :", font=font_label).grid(row=3, column=0, sticky="e", padx=5, pady=5)
        article_combo = ttk.Combobox(root, textvariable=article_var, font=font_entry, state="readonly")
        article_combo.grid(row=3, column=1, columnspan=2, sticky="we", padx=5, pady=5)


        tk.Label(root, text="Employé :", font=font_label).grid(row=4, column=0, sticky="e", padx=5, pady=5)
        employe_combo = ttk.Combobox(root, textvariable=employe_var, font=font_entry, values=list(employes.keys()), state="normal")
        employe_combo.bind("<KeyRelease>", lambda event: filter_employe_options(event, employe_combo, list(employes.keys())))
        employe_combo.grid(row=4, column=1, columnspan=2, sticky="we", padx=5, pady=5)

        tk.Label(root, text="Type de document :", font=font_label).grid(row=5, column=0, sticky="e", padx=5, pady=5)
        type_doc_combo = ttk.Combobox(root, textvariable=type_document_var, font=font_entry, values=["Document", "Film", "Carton"], state="readonly")
        type_doc_combo.grid(row=5, column=1, columnspan=2, sticky="we", padx=5, pady=5)
        

        type_palette_row = 6
        type_palette_label = tk.Label(root, text="Type de palette :", font=font_label)
        type_palette_combo = ttk.Combobox(root, textvariable=type_palette_var, font=font_entry, values=list(types_palettes.keys()), state="readonly")

        poids_10_row = 7
        nb_cartons_row = 7
        nb_docs_row = 8

        poids_palette_label = tk.Label(root, text="Poids de la palette (kg) :", font=font_label)
        poids_palette_label.grid(row=9, column=0, sticky="e", padx=5, pady=5)
        poids_palette_entry = tk.Entry(root, textvariable=poids_palette_var, font=font_entry)
        poids_palette_entry.grid(row=9, column=1, columnspan=2, sticky="we", padx=5, pady=5)

        quantite_label = tk.Label(root, text="Quantité :", font=font_label)
        quantite_entry = tk.Entry(root, textvariable=quantite_directe_var, font=font_entry)

        tk.Button(root, text="Imprimer", font=font_entry, command=generer_et_imprimer, bg="#4CAF50", fg="white").grid(row=10, column=0, columnspan=3, pady=20)

        tk.Button(root, text="Retour au menu", font=font_entry, command=lambda: retour_menu(root), bg="#E53935", fg="white").grid(row=11, column=0, columnspan=3, pady=10)

        root.columnconfigure(1, weight=1)

        type_document_var.trace_add("write", update_fields)
        update_fields()

        root.mainloop()

    # Mode manuel si connexion imppossible à GDC
    if mode == "manuel":    
        operation_var = tk.StringVar()
        client_var = tk.StringVar()
        action_var = tk.StringVar()
        designation_var = tk.StringVar()
        quantite_directe_var = tk.IntVar()

        tk.Label(root, text="Numéro d'opération :", font=font_label).grid(row=0, column=0, sticky="e", padx=5, pady=5)
        tk.Entry(root, textvariable=num_operation_var, font=font_entry).grid(row=0, column=1, columnspan=2, sticky="we", padx=5, pady=5)

        tk.Label(root, text="Nom de l'opération :", font=font_label).grid(row=1, column=0, sticky="e", padx=5, pady=5)
        tk.Entry(root, textvariable=operation_var, font=font_entry).grid(row=1, column=1, columnspan=2, sticky="we", padx=5, pady=5)

        tk.Label(root, text="Client :", font=font_label).grid(row=2, column=0, sticky="e", padx=5, pady=5)
        tk.Entry(root, textvariable=client_var, font=font_entry).grid(row=2, column=1, columnspan=2, sticky="we", padx=5, pady=5)

        tk.Label(root, text="Action client :", font=font_label).grid(row=3, column=0, sticky="e", padx=5, pady=5)
        tk.Entry(root, textvariable=action_var, font=font_entry).grid(row=3, column=1, columnspan=2, sticky="we", padx=5, pady=5)

        tk.Label(root, text="Référence :", font=font_label).grid(row=4, column=0, sticky="e", padx=5, pady=5)
        tk.Entry(root, textvariable=article_var, font=font_entry).grid(row=4, column=1, columnspan=2, sticky="we", padx=5, pady=5)

        tk.Label(root, text="Employé :", font=font_label).grid(row=5, column=0, sticky="e", padx=5, pady=5)
        tk.Entry(root, textvariable=employe_var, font=font_entry).grid(row=5, column=1, columnspan=2, sticky="we", padx=5, pady=5)

        tk.Label(root, text="Désignation :", font=font_label).grid(row=6, column=0, sticky="e", padx=5, pady=5)
        tk.Entry(root, textvariable=designation_var, font=font_entry).grid(row=6, column=1, columnspan=2, sticky="we", padx=5, pady=5)

        tk.Label(root, text="Type de document :", font=font_label).grid(row=7, column=0, sticky="e", padx=5, pady=5)
        type_doc_combo = ttk.Combobox(root, textvariable=type_document_var, font=font_entry, values=["Document", "Film", "Carton", "Livraison"], state="readonly")
        type_doc_combo.grid(row=7, column=1, columnspan=2, sticky="we", padx=5, pady=5)
       
        type_palette_row = 8
        type_palette_label = tk.Label(root, text="Type de palette :", font=font_label)
        type_palette_combo = ttk.Combobox(root, textvariable=type_palette_var, font=font_entry, values=list(types_palettes.keys()), state="readonly")

        poids_10_row = 9
        nb_cartons_row = 9
        nb_docs_row = 10

        quantite_label = tk.Label(root, text="Quantité :", font=font_label)
        quantite_entry = tk.Entry(root, textvariable=quantite_directe_var, font=font_entry)
        poids_palette_label = tk.Label(root, text="Poids de la palette (kg) :", font=font_label)
        poids_palette_label.grid(row=11, column=0, sticky="e", padx=5, pady=5)
        poids_palette_entry = tk.Entry(root, textvariable=poids_palette_var, font=font_entry)
        poids_palette_entry.grid(row=11, column=1, columnspan=2, sticky="we", padx=5, pady=5)

        tk.Button(root, text="Imprimer", font=font_entry, command=generer_et_imprimer, bg="#4CAF50", fg="white").grid(row=13, column=0, columnspan=3, pady=20)

        tk.Button(root, text="Retour au menu", font=font_entry, command=lambda: retour_menu(root), bg="#E53935", fg="white").grid(row=14, column=0, columnspan=3, pady=10)

        root.columnconfigure(1, weight=1)

        type_document_var.trace_add("write", update_fields)
        update_fields()

        root.mainloop()

#====================================================================================================================
# Lancer l’application
start_mode_selection()
