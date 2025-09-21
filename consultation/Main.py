import sys
import os
import pymysql
import csv
import bcrypt
import logging
import traceback
from logging.handlers import RotatingFileHandler
from dotenv import load_dotenv
from PyQt5.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QTableWidget, QTableWidgetItem,
    QHBoxLayout, QPushButton, QLineEdit, QLabel, QComboBox, QFileDialog,
    QMessageBox, QHeaderView, QAbstractItemView, QInputDialog, QDialog,
    QAbstractScrollArea
)
from PyQt5.QtCore import Qt, QTimer
from datetime import datetime, timedelta
from collections import Counter


# Configuration des en-têtes pour les tables InfoPalette et SPR_Palette
HEADERS_INFOPALETTE = [
    "NumPalette",
    "NomClient",
    "Article",
    "Quantite",
    "Emplacement",
    "Date_Dernier_MVT",
    "Statut",
    "Date_Modif_Statut",
    "Utilisateur_Modif_Statut"
]

HEADERS_SPR_PALETTE = HEADERS_INFOPALETTE.copy()

# Fonction qui intercepte les exceptions non gérées et les enregistre dans un fichier texte
def excepthook(exctype, value, tb):
    try:
        with open("error_log.txt", "a", encoding="utf-8") as f:
            f.write("=== Exception non interceptée ===\n")
            import traceback
            traceback.print_exception(exctype, value, tb, file=f)
    except Exception:
        pass
    try:
        # Vérifie que QApplication a été instancié
        if QApplication.instance():
            QMessageBox.critical(None, "Erreur critique", "Une erreur inattendue est survenue.\nVoir error_log.txt pour plus de détails.")
        else:
            print("Erreur inattendue, voir error_log.txt.")
    except Exception:
        print("Erreur inattendue, voir error_log.txt.")

sys.excepthook = excepthook

log_file = "error_log.txt"
handler = RotatingFileHandler(log_file, maxBytes=500_000, backupCount=3, encoding="utf-8")
logging.basicConfig(
    level=logging.ERROR,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[handler]
)

# Fonction qui affiche une boîte de dialogue d'erreur à l'utilisateur
#Si l'interface graphique n'est pas disponible, affiche l'erreur dans la console
def afficher_erreur_utilisateur(message, parent=None):
    try:
        QMessageBox.critical(parent, "Erreur", f"{message}\n\n(Voir error_log.txt pour plus de détails)")
    except Exception:
        print(f"Erreur: {message}\n(Voir error_log.txt pour plus de détails)")

# Fonction utilitaire pour accéder aux fichiers dans le dossier 'data'
def resource_path(filename):
    return os.path.join(os.path.dirname(sys.executable if getattr(sys, 'frozen', False) else __file__), "Data", filename)

load_dotenv(resource_path(".env"))

#MySQL
mysql_user = os.getenv("MYSQL_USER")
mysql_password = os.getenv("MYSQL_PASSWORD")
mysql_host = os.getenv("MYSQL_HOST")
mysql_port = int(os.getenv("MYSQL_PORT", "3307"))
mysql_bname = os.getenv("MYSQL_BNAME")

# Fonction qui crée une connexion à la base de données MySQL à partir des variables d'environnement
#Affiche une erreur utilisateur et arrête l'application si la connexion échoue
def get_connection():
    try:
        return pymysql.connect(
            host=mysql_host,
            port=mysql_port,
            user=mysql_user,
            password=mysql_password,
            database=mysql_bname,
            charset="utf8mb4",
            cursorclass=pymysql.cursors.DictCursor
        )
    except Exception as e:
        logging.error(f"Erreur de connexion MySQL : {e}")
        afficher_erreur_utilisateur(f"Impossible de se connecter à la base de données:\n{e}")
        sys.exit(1)


# Fonction utilitaire qui normalise une ligne récupérée depuis la base
#Remplit les valeurs manquantes avec "" ou None pour les dates et quantités
def normalize_row(row, colonnes_list):
    result = {}
    for col in colonnes_list:
        val = row.get(col, "")
        if col.lower().startswith("date") or col.lower() in ["quantite"]:
            if val in (None, "", "None"):
                result[col] = None
            else:
                result[col] = val
        else:
            result[col] = val if val is not None else ""
    return result

# Boîte de dialogue pour la connexion utilisateur
#Utilise le fichier users.json pour vérifier l'identifiant et le mot de passe (hashé avec bcrypt)
class LoginDialog(QDialog):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Connexion")
        self.setGeometry(600, 300, 300, 150)
        layout = QVBoxLayout()

        self.user_input = QLineEdit()
        self.user_input.setPlaceholderText("Nom d'utilisateur")
        self.pass_input = QLineEdit()
        self.pass_input.setPlaceholderText("Mot de passe")
        self.pass_input.setEchoMode(QLineEdit.Password)

        self.connect_button = QPushButton("Se connecter")
        self.connect_button.clicked.connect(self.verify_credentials)

        layout.addWidget(QLabel("Nom d'utilisateur"))
        layout.addWidget(self.user_input)
        layout.addWidget(QLabel("Mot de passe"))
        layout.addWidget(self.pass_input)
        layout.addWidget(self.connect_button)

        self.setLayout(layout)
        self.valid_username = None
        self.valid_role = None

    # Méthode pour vérifier les identifiants de l'utilisateur
    def verify_credentials(self):
        username = self.user_input.text().strip()
        password = self.pass_input.text().strip()

        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                cursor.execute("SELECT Password, Role FROM Users WHERE Login = %s", (username,))
                result = cursor.fetchone()
            conn.close()
            if result and bcrypt.checkpw(password.encode(), result["Password"].encode()):
                self.valid_username = username
                self.valid_role = result.get("Role", "utilisateur")
                logging.info(f"Connexion réussie pour l'utilisateur : {username} ({self.valid_role})")
                self.accept()
                return
        except Exception as e:
            logging.error(f"Erreur dans la vérification des identifiants : {e}")

        QMessageBox.warning(self, "Erreur", "Identifiant ou mot de passe incorrect")

    # Méthode pour obtenir le nom d'utilisateur et le rôle après une connexion réussie
    def get_username(self):
        return self.valid_username

    # Méthode pour obtenir le rôle de l'utilisateur après une connexion réussie
    def get_role(self):
        return self.valid_role

# Classe principale de l'application qui gère l'interface utilisateur et les interactions avec la base de données
class StockApp(QWidget):
    # Initialisation de l'application
    def consulter_table(self, table_name):
        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                cursor.execute(f"SELECT * FROM {table_name}")
                rows = cursor.fetchall()
            conn.close()
        except Exception as e:
            logging.error(f"Erreur lors de la consultation de la table {table_name}: {e}")
            QMessageBox.critical(self, "Erreur", f"Erreur lors de la consultation de la table :\n{e}")
            return

        if not rows:
            QMessageBox.information(self, "Consultation", f"Aucune donnée trouvée dans {table_name}.")
            return

        dialog = QDialog(self)
        dialog.setWindowTitle(f"Consultation de {table_name}")
        dialog.setGeometry(200, 200, 1200, 600)
        layout = QVBoxLayout(dialog)

        if table_name == "SPR_Palette":
            headers = HEADERS_SPR_PALETTE
        else:
            headers = HEADERS_INFOPALETTE

        table = QTableWidget(len(rows), len(headers))
        table.setHorizontalHeaderLabels(headers)
        table.setSelectionBehavior(QAbstractItemView.SelectRows)
        table.setSelectionMode(QAbstractItemView.MultiSelection)
        for row_idx, row in enumerate(rows):
            for col_idx, key in enumerate(headers):
                value = row.get(key, "") if row.get(key, "") is not None else ""
                item = QTableWidgetItem(str(value))
                table.setItem(row_idx, col_idx, item)
            if row_idx % 2 == 0:
                for col_idx in range(len(headers)):
                    table.item(row_idx, col_idx).setBackground(Qt.lightGray)
        table.resizeColumnsToContents()
        table.horizontalHeader().setSectionResizeMode(QHeaderView.Stretch)
        layout.addWidget(table)

        if table_name == "SPR_Palette" and self.role == "admin":
            bouton_restaure = QPushButton("Restaurer palettes sélectionnées")
            bouton_restaure.clicked.connect(lambda: self.restaurer_palettes_selectionnees(table, dialog, headers))
            layout.addWidget(bouton_restaure)
            bouton_purge_archives = QPushButton("Purger archives anciennes (export CSV)")
            bouton_purge_archives.clicked.connect(lambda: self.purger_archives_anciennes(table, dialog, headers))
            layout.addWidget(bouton_purge_archives)

        dialog.exec_()
    
    # Méthode pour restaurer les palettes sélectionnées dans la table SPR_Palette
    def restaurer_palettes_selectionnees(self, table, dialog, colonnes_list):
        self.status_label.setText("Traitement en cours…")
        QApplication.processEvents()
        rows_to_restore = []
        for idx in [index.row() for index in table.selectionModel().selectedRows()]:
            row_data = {}
            for col, key in enumerate(colonnes_list):
                val = table.item(idx, col).text()
                if key.lower().startswith("date") and (val.lower() == "none" or val == ""):
                    row_data[key] = None
                else:
                    row_data[key] = val
            row_data = normalize_row(row_data, colonnes_list)
            rows_to_restore.append(row_data)
        if not rows_to_restore:
            QMessageBox.warning(dialog, "Erreur", "Aucune ligne sélectionnée")
            return
        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                for row in rows_to_restore:
                    cursor.execute("SELECT COUNT(*) AS c FROM InfoPalette WHERE NumPalette=%s", (row.get("NumPalette",""),))
                    if cursor.fetchone()["c"]:
                        continue
                    cursor.execute(f"INSERT INTO InfoPalette ({', '.join(colonnes_list)}) VALUES ({', '.join(['%s'] * len(colonnes_list))})",tuple(row.get(col, None) for col in colonnes_list))
                    cursor.execute("DELETE FROM SPR_Palette WHERE NumPalette=%s", (row.get("NumPalette",""),))
            conn.commit()
            conn.close()
        except Exception as e:
            logging.error(f"Erreur lors de la restauration de palettes: {e}")
            QMessageBox.critical(dialog, "Erreur", f"Erreur lors de la restauration :\n{e}")
            return
        QMessageBox.information(dialog, "Restauration", f"{len(rows_to_restore)} palettes restaurées (conflits ignorés).")
        self.status_label.setText("Restauration terminée.")
        dialog.accept()
        self.load_data()

    #Méthode qui s'occupe de l'affichage de la fenêtre principale de l'application
    def __init__(self, username, role):
        super().__init__()
        self.utilisateur = username
        self.role = role
        self.setWindowTitle(f"Consultation des stocks - {username} ({role})")
        self.setGeometry(100, 100, 1600, 800)
        self.layout = QVBoxLayout()
        self.setLayout(self.layout)

        # Timer pour le debounce des filtres
        self.debounce_timer = QTimer(self)
        self.debounce_timer.setSingleShot(True)
        self.debounce_timer.timeout.connect(self.mettre_a_jour_references)

        # Label pour les indicateurs
        self.indicateurs_label = QLabel()
        self.layout.addWidget(self.indicateurs_label)

        # Configuration des filtres
        self.filters_layout = QHBoxLayout()
    
        self.client_input = QComboBox()
        self.client_input.setEditable(True)
        self.client_input.lineEdit().setPlaceholderText("Filtrer par client")
        self.client_input.editTextChanged.connect(self.on_client_input_changed)
        self.charger_liste_clients()
        self.client_input.setFixedWidth(450)

        self.reference_input = QComboBox()
        self.reference_input.setEditable(True)
        self.reference_input.lineEdit().setPlaceholderText("Filtrer par référence")
        self.reference_input.setFixedWidth(600)

        self.statut_input = QComboBox()
        self.statut_input.addItem("Tous")
        self.statut_input.addItems(["En stock", "A Détruire", "Détruite", "En prod", "A Renvoyer", "Renvoyé", "A Inventorier"])
        self.statut_input.setFixedWidth(300)

        self.num_palette_input = QLineEdit()
        self.num_palette_input.setPlaceholderText("Filtrer par numéro de palette")
        self.num_palette_input.setFixedWidth(300)

        self.filtrer_button = QPushButton("Filtrer")
        self.filtrer_button.clicked.connect(self.filtrer)
        self.filtrer_button.setFixedWidth(100)

        # Connexion de la touche Entrée
        self.client_input.lineEdit().returnPressed.connect(self.filtrer)
        self.reference_input.lineEdit().returnPressed.connect(self.filtrer)
        self.num_palette_input.returnPressed.connect(self.filtrer)

        # Ajout des widgets au layout
        self.filters_layout.addWidget(self.num_palette_input)
        self.filters_layout.addWidget(self.client_input)
        self.filters_layout.addWidget(self.reference_input)
        self.filters_layout.addWidget(self.statut_input) 
        self.filters_layout.addWidget(self.filtrer_button)

        self.layout.addLayout(self.filters_layout)

        # Configuration de la table avec coloration alternée native
        self.table = QTableWidget()
        self.table.setAlternatingRowColors(True)  # Activation de la coloration alternée native
        self.table.setStyleSheet("""
            QTableWidget {
                alternate-background-color: #89cff0;  /* Couleur pour les lignes paires */
                gridline-color: #d0d0d0;          /* Couleur des grilles */
            }
            QTableWidget::item {
                padding: 3px;                     /* Marge interne */
            }
            QTableWidget::item:selected {
                background-color: #a0a0a0;        /* Couleur de sélection */
                color: white;
            }
        """)
    
        # Configuration des propriétés de la table
        self.table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.table.setSelectionMode(QAbstractItemView.MultiSelection)
        self.table.horizontalHeader().setSectionResizeMode(QHeaderView.Interactive)
        self.table.setSizeAdjustPolicy(QAbstractScrollArea.AdjustToContents)
        self.table.horizontalHeader().sectionClicked.connect(self.trier_donnees)
        self.layout.addWidget(self.table)

        # Configuration des boutons en bas
        self.bottom_layout = QHBoxLayout()
    
        self.export_button = QPushButton("Exporter CSV")
        self.export_button.clicked.connect(self.export_csv)
    
        self.changer_statut_button = QPushButton("Changer le statut")
        self.changer_statut_button.clicked.connect(self.changer_statut)
    
        self.alert_button = QPushButton("Afficher alertes")
        self.alert_button.clicked.connect(self.afficher_alertes)
    
        self.purge_button = QPushButton("Purger les palettes")
        self.purge_button.clicked.connect(self.purger_palettes)
    
        self.historique_mvt_button = QPushButton("Voir historique des mouvements")
        self.historique_mvt_button.clicked.connect(self.afficher_historique_mvt)
    
        self.historique_statut_button = QPushButton("Voir historique des statuts")
        self.historique_statut_button.clicked.connect(self.afficher_historique_statut)
    
        self.spr_button = QPushButton("Voir palettes archivées")
        self.spr_button.clicked.connect(lambda: self.consulter_table("SPR_Palette"))
    
        # Ajout des boutons
        buttons = [
            self.export_button,
            self.changer_statut_button,
            self.alert_button,
            self.purge_button,
            self.historique_mvt_button,
            self.historique_statut_button,
            self.spr_button
        ]
    
        for btn in buttons:
            self.bottom_layout.addWidget(btn)
    
        self.layout.addLayout(self.bottom_layout)

        # Gestion des droits utilisateur
        if self.role == "utilisateur":
            self.changer_statut_button.setEnabled(False)
            self.export_button.setEnabled(False)
            self.purge_button.setEnabled(False)
            self.spr_button.setEnabled(False)
        elif self.role == "responsable":
            self.purge_button.setEnabled(False)
            self.spr_button.setEnabled(False)

        # Labels supplémentaires
        self.role_label = QLabel(f"Utilisateur connecté : {self.utilisateur} ({self.role})")
        self.layout.addWidget(self.role_label)

        self.status_label = QLabel()
        self.layout.addWidget(self.status_label)

        # Chargement initial des données
        self.load_data()

    # Méthode appelée lorsque le texte du champ client change et permet de mettre à jour les références disponibles avec un petit délai
    def on_client_input_changed(self):
        self.debounce_timer.start(300)

    # Fonction qui charge les données de la table InfoPalette depuis la base et met à jour l'affichage
    #Affiche une erreur si le chargement échoue
    def load_data(self):
        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                cursor.execute("""SELECT * FROM InfoPalette""")
                self.data = cursor.fetchall()
            conn.close()
        except Exception as e:
            logging.error(f"Erreur lors du chargement des données: {e}")
            QMessageBox.critical(self, "Erreur", f"Erreur lors du chargement des données :\n{e}")
            self.data = []
        self.afficher_table(self.data)
        self.mettre_a_jour_indicateurs()

    #Méthode qui s'occupe de trier les données de la table en fonction de la colonne cliquée
    def trier_donnees(self, colonne):
        if not hasattr(self, 'displayed_data') or not self.displayed_data:
            return

        header = HEADERS_INFOPALETTE[colonne]
    
        self.table.setSortingEnabled(False)
    
        if hasattr(self, "_dernier_tri") and self._dernier_tri == (colonne, True):
            asc = False
        else:
            asc = True
        self._dernier_tri = (colonne, asc)

        def parse_value(val):
            try:
                return datetime.strptime(val, "%Y-%m-%d %H:%M:%S")
            except (ValueError, TypeError):
                try:
                    return float(val)
                except (ValueError, TypeError):
                    try:
                        return str(val).lower()
                    except Exception:
                        return ""

        def safe_key(row):
            val = row.get(header, "")
            parsed = parse_value(val)
            return (type(parsed).__name__, parsed)

        self.displayed_data.sort(key=safe_key, reverse=not asc)
        
        self.table.setRowCount(0)
        self.table.setRowCount(len(self.displayed_data))
    
        for row_idx, row in enumerate(self.displayed_data):
            for col_idx, key in enumerate(HEADERS_INFOPALETTE):
                value = str(row.get(key, "")) if row.get(key) is not None else ""
                self.table.setItem(row_idx, col_idx, QTableWidgetItem(value))

        self.table.setSortingEnabled(True)

    # Méthode pour lister dynamiquement les références disponibles en fonction du client saisi
    def mettre_a_jour_references(self):
        client = self.client_input.currentText().strip()
        if not client:
            return
        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                cursor.execute("SELECT DISTINCT Article FROM InfoPalette WHERE NomClient LIKE %s", (f"%{client}%",))
                articles = [r.get("Article", "") for r in cursor.fetchall()]
            conn.close()
        except Exception as e:
            logging.error(f"Erreur lors de la mise à jour des références: {e}")
            QMessageBox.warning(self, "Erreur", f"Erreur lors de la mise à jour des références :\n{e}")
            articles = []
        current_text = self.reference_input.currentText()
        self.reference_input.clear()
        self.reference_input.addItems(articles)
        self.reference_input.setCurrentIndex(-1)
        self.reference_input.setEditText(current_text)

    # Méthode pour charger la liste des clients distincts dans le champ de saisie
    def charger_liste_clients(self):
        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                cursor.execute("SELECT DISTINCT NomClient FROM InfoPalette WHERE NomClient IS NOT NULL AND NomClient != ''")
                clients = [r["NomClient"] for r in cursor.fetchall()]
            conn.close()
        
            current_text = self.client_input.currentText()
            self.client_input.clear()
            self.client_input.addItems(sorted(clients))
            self.client_input.setCurrentIndex(-1)
            self.client_input.setEditText(current_text)
        except Exception as e:
            logging.error(f"Erreur lors du chargement des clients: {e}")

    # Méthode pour purger les palettes archivées
    def purger_archives_anciennes(self, table, dialog, colonnes_list):
        seuil, ok = QInputDialog.getInt(dialog, "Choix du seuil", "Nombre d'années à conserver :", 3, 1, 20)
        if not ok:
            return
        date_limite = datetime.now() - timedelta(days=365 * seuil)
        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                cursor.execute("SELECT * FROM SPR_Palette WHERE Date_Dernier_MVT < %s", (date_limite,))
                anciennes = cursor.fetchall()
            if not anciennes:
                QMessageBox.information(dialog, "Purge", "Aucune archive trop ancienne à purger.")
                return

            path, _ = QFileDialog.getSaveFileName(dialog, "Exporter les archives en CSV", "palettes_archivees.csv", "CSV files (*.csv)")
            if path:
                try:
                    with open(path, "w", newline="", encoding="utf-8") as f:
                        writer = csv.writer(f, delimiter=';')
                        writer.writerow(colonnes_list)
                        for row in anciennes:
                            row_norm = normalize_row(row, colonnes_list)
                            writer.writerow([row_norm.get(col, "") for col in colonnes_list])
                except Exception as e:
                    QMessageBox.warning(dialog, "Erreur export", f"Erreur lors de l'export : {e}")
                    return

            confirm = QMessageBox.question(
                dialog,
                "Confirmation",
                f"{len(anciennes)} palettes archivées seront supprimées définitivement.\nContinuer ?",
                QMessageBox.Yes | QMessageBox.No
            )
            if confirm != QMessageBox.Yes:
                return

            with conn.cursor() as cursor:
                cursor.execute("DELETE FROM SPR_Palette WHERE Date_Dernier_MVT < %s", (date_limite,))
            conn.commit()
            QMessageBox.information(dialog, "Purge", f"{len(anciennes)} palettes supprimées (CSV : {os.path.basename(path)})")
            dialog.accept()
        except Exception as e:
            QMessageBox.critical(dialog, "Erreur", f"Erreur lors de la suppression :\n{e}")
        finally:
            try:
                conn.close()
            except Exception:
                pass
        self.load_data()

    #Méthode qui s'occupe de l'affichage des tables
    def afficher_table(self, rows):
        self.displayed_data = list(rows)
        headers = HEADERS_INFOPALETTE
    
        self.table.setUpdatesEnabled(False)
        self.table.setSortingEnabled(False)
    
        self.table.clear()
        self.table.setRowCount(0)
        self.table.setColumnCount(len(headers))
        self.table.setHorizontalHeaderLabels(headers)
        self.table.setRowCount(len(self.displayed_data))

        for row_idx, row in enumerate(self.displayed_data):
            for col_idx, key in enumerate(headers):
                value = str(row.get(key, "")) if row.get(key) is not None else ""
                item = QTableWidgetItem(value)
                self.table.setItem(row_idx, col_idx, item)

        # Configuration du redimensionnement
        self.table.resizeColumnsToContents()
    
        # Étirement des colonnes principales si besoin
        table_width = self.table.viewport().width()
        total_width = sum(self.table.columnWidth(i) for i in range(self.table.columnCount()))
    
        if total_width < table_width:
            stretch_cols = [i for i, h in enumerate(headers) if h in ['NomClient', 'Article', 'Emplacement']]
            for col in stretch_cols:
                self.table.horizontalHeader().setSectionResizeMode(col, QHeaderView.Stretch)
    
        self.table.horizontalHeader().setStretchLastSection(True)
    
        self.table.setUpdatesEnabled(True)
        self.table.setSortingEnabled(True)
        self.status_label.setText(f"{len(self.displayed_data)} palettes affichées")

    # Méthode pour mettre à jour les indicateurs de statut des palettes
    def mettre_a_jour_indicateurs(self):
        compte = Counter([r.get("Statut", "") for r in self.data])
        texte = " | ".join([f"{k}: {v}" for k, v in compte.items()])
        self.indicateurs_label.setText(f"Palettes par statut: {texte}")

    # Méthode pour filtrer les données affichées dans la table en fonction des critères saisis
    def filtrer(self):
        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                cursor.execute("SELECT * FROM InfoPalette")
                self.data = cursor.fetchall()
            conn.close()
        except Exception as e:
            logging.error(f"Erreur lors du chargement des données: {e}")
            return

        num_palette = self.num_palette_input.text().lower()
        client = self.client_input.currentText().lower()
        ref = self.reference_input.currentText().lower()
        statut = self.statut_input.currentText()

        filtered = [
            row for row in self.data
            if (not num_palette or num_palette in str(row.get('NumPalette', '')).lower()) and
            (not client or client in str(row.get('NomClient', '')).lower()) and
            (not ref or ref in str(row.get('Article', '')).lower()) and
            (statut.lower() == "tous" or str(row.get('Statut', '')).lower() == statut.lower())
        ]
    
        if hasattr(self, '_dernier_tri'):
            colonne, asc = self._dernier_tri
            header = HEADERS_INFOPALETTE[colonne]
            filtered.sort(
                key=lambda x: str(x.get(header, '')).lower(),
                reverse=not asc
            )
    
        self.displayed_data = filtered
        self.afficher_table(filtered)

    #Méthode qui gère l'export des données affichées en CSV
    def export_csv(self):
        if not hasattr(self, "displayed_data") or not self.displayed_data or len(self.displayed_data) == 0:
            QMessageBox.information(self, "Export", "Aucune donnée à exporter.")
            return
        path, _ = QFileDialog.getSaveFileName(self, "Exporter en CSV", "stock.csv", "CSV files (*.csv)")
        if not path:
            return
        try:
            with open(path, 'w', newline='', encoding='utf-8') as f:
                writer = csv.writer(f, delimiter=';')
                writer.writerow(HEADERS_INFOPALETTE)
                for row in self.displayed_data:
                    row_normalized = normalize_row(row, HEADERS_INFOPALETTE)
                    writer.writerow([row_normalized.get(col, "") if row_normalized.get(col, "") is not None else "" for col in HEADERS_INFOPALETTE])
            logging.info(f"{self.utilisateur} a exporté les données en CSV vers {path}")
            QMessageBox.information(self, "Export", "Export CSV réussi !")
        except Exception as e:
            logging.error(f"Erreur lors de l'export CSV: {e}")
            QMessageBox.critical(self, "Erreur", f"Erreur lors de l'export CSV :\n{e}")

    #Méthode qui permet de changer le statut des palettes sélectionnées
    def changer_statut(self):
        selected_rows = [index.row() for index in self.table.selectionModel().selectedRows()]
        if not selected_rows:
            QMessageBox.warning(self, "Erreur", "Aucune ligne sélectionnée")
            return

        statuts = ["En stock", "A Détruire", "Détruite", "En prod", "A Renvoyer", "Renvoyé", "A Inventorier"]
        statut, ok = QInputDialog.getItem(self, "Changer statut", "Nouveau statut", statuts, 0, False)
        if not ok:
            return

        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                for row_idx in selected_rows:
                    item = self.table.item(row_idx, 0)
                    if item is None:
                        continue
                    num_palette = item.text()

                    cursor.execute("""
                        UPDATE InfoPalette
                        SET Statut = %s, Date_Modif_Statut = %s, Utilisateur_Modif_Statut = %s
                        WHERE NumPalette = %s
                    """, (statut, now, self.utilisateur, num_palette))

                    cursor.execute("""
                        INSERT INTO STT_Palette (NumPalette, Statut, Date_Modif_Statut, Utilisateur_Modif_Statut)
                        VALUES (%s, %s, %s, %s)
                    """, (num_palette, statut, now, self.utilisateur))

                    if statut in ("En prod", "Détruite", "Renvoyé"):
                        cursor.execute("""
                        SELECT Emplacement FROM InfoPalette WHERE NumPalette = %s
                    """, (num_palette,))
                    emplacement_row = cursor.fetchone()
                    if emplacement_row and emplacement_row.get("Emplacement"):
                        emplacement = emplacement_row["Emplacement"]
                        cursor.execute("""
                            UPDATE EmplacementEntrepot
                            SET Etat = 'Libre'
                            WHERE Emplacement = %s
                        """, (emplacement,))

                    if statut == "En stock":
                        cursor.execute("""
                            SELECT Emplacement FROM InfoPalette WHERE NumPalette = %s
                        """, (num_palette,))
                        emplacement_row = cursor.fetchone()
                        if emplacement_row and emplacement_row.get("Emplacement"):
                            emplacement = emplacement_row["Emplacement"]
                            cursor.execute("""
                                UPDATE EmplacementEntrepot
                                SET Etat = 'Occupé'
                                WHERE Emplacement = %s
                            """, (emplacement,))

            conn.commit()
            conn.close()
        except Exception as e:
            logging.error(f"Erreur lors du changement de statut: {e}")
            QMessageBox.critical(self, "Erreur", f"Erreur lors du changement de statut :\n{e}")
            return

        logging.info(f"{self.utilisateur} a modifié le statut de {len(selected_rows)} palettes en '{statut}'")

        # Sauvegarde des filtres avant rechargement
        client_actuel = self.client_input.currentText()
        ref_actuel = self.reference_input.currentText()
        num_palette_actuel = self.num_palette_input.text()
        statut_actuel = self.statut_input.currentText()

        self.load_data()

        # Réapplication des filtres
        self.client_input.setCurrentText(client_actuel)
        self.reference_input.setCurrentText(ref_actuel)
        self.num_palette_input.setText(num_palette_actuel)
        self.statut_input.setCurrentText(statut_actuel)

        self.filtrer()
        
        QMessageBox.information(self, "Succès", f"Statut mis à jour pour {len(selected_rows)} palettes.")

    # Méthode pour afficher les alertes de palettes inactives depuis plus de 60 jours
    def afficher_alertes(self):
        seuil = datetime.now() - timedelta(days=60)
        inactives = []
        for r in self.data:
            date_str = r.get("Date_Dernier_MVT")
            if not date_str:
                continue
            try:
                dt = datetime.strptime(date_str, "%Y-%m-%d %H:%M:%S")
                if dt < seuil:
                    inactives.append(r)
            except Exception as e:
                logging.error(f"Erreur de parsing date sur Date_Dernier_MVT='{date_str}': {e}")
                continue
        if inactives:
            QMessageBox.information(self, "Alertes", f"{len(inactives)} palettes inactives depuis plus de 60 jours.")
        else:
            QMessageBox.information(self, "Alertes", "Aucune palette inactive")

    # Méthode pour purger les palettes inactive et dans un état "final" (A Détruire, En Prod ou Renvoyé) depuis plus de 3 mois
    def purger_palettes(self):
        self.status_label.setText("Traitement en cours…")
        QApplication.processEvents()
        statuts_a_purger = ("A Détruire", "En prod", "Renvoyé")
        confirm = QMessageBox.question(
            self, "Confirmation",
            "Êtes-vous sûr de vouloir purger les palettes \"En prod\", \"Détruite\" ou \"Renvoyé\" depuis plus de 3 mois ?",
            QMessageBox.Yes | QMessageBox.No
        )
        if confirm != QMessageBox.Yes:
            return
        seuil = datetime.now() - timedelta(days=90)
        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                cursor.execute("""
                    SELECT * FROM InfoPalette
                    WHERE Statut IN (%s, %s, %s) AND Date_Modif_Statut < %s
                """, (*statuts_a_purger, seuil))
                lignes = cursor.fetchall()
                if not lignes:
                    QMessageBox.information(self, "Purge", "Aucune palette à purger.")
                    return
                colonnes_list = HEADERS_INFOPALETTE
                for ligne in lignes:
                    normed = normalize_row(ligne, colonnes_list)
                    cursor.execute(f"INSERT INTO SPR_Palette ({', '.join(colonnes_list)}) VALUES ({', '.join(['%s'] * len(colonnes_list))})",tuple(normed.get(col, None) for col in colonnes_list))
                cursor.execute(f"DELETE FROM InfoPalette WHERE NumPalette IN ({', '.join(['%s'] * len(lignes))})", [l.get("NumPalette", "") for l in lignes])
                cursor.execute(f"DELETE FROM STT_Palette WHERE NumPalette IN ({', '.join(['%s'] * len(lignes))})", [l.get("NumPalette", "") for l in lignes])
                cursor.execute(f"DELETE FROM MVT_Palette WHERE NumPalette IN ({', '.join(['%s'] * len(lignes))})", [l.get("NumPalette", "") for l in lignes])
            conn.commit()
            conn.close()
        except Exception as e:
            logging.error(f"Erreur lors de la purge des palettes: {e}")
            QMessageBox.critical(self, "Erreur", f"Erreur lors de la purge :\n{e}")
            return
        QMessageBox.information(self, "Purge", f"{len(lignes)} palettes purgées avec succès.")
        self.status_label.setText("Restauration terminée.")
        self.load_data()

    # Méthode qui permet d'afficher l'historique de statut d'une palette sélectionnée
    def afficher_historique_statut(self):
        selected_rows = [index.row() for index in self.table.selectionModel().selectedRows()]
        if not selected_rows:
            QMessageBox.warning(self, "Erreur", "Aucune ligne sélectionnée")
            return

        num_palette = self.table.item(selected_rows[0], 0).text()

        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                cursor.execute("""
                    SELECT Statut, Date_Modif_Statut, Utilisateur_Modif_Statut
                    FROM STT_Palette
                    WHERE NumPalette = %s
                    ORDER BY Date_Modif_Statut DESC
                """, (num_palette,))
                historiques = cursor.fetchall()
            conn.close()
        except Exception as e:
            logging.error(f"Erreur lors de l'affichage de l'historique de statut: {e}")
            QMessageBox.critical(self, "Erreur", f"Erreur lors de l'affichage de l'historique :\n{e}")
            return

        dialog = QDialog(self)
        dialog.setWindowTitle(f"Historique de statut - Palette {num_palette}")
        dialog.setGeometry(300, 300, 600, 300)
        layout = QVBoxLayout(dialog)

        table = QTableWidget(len(historiques), 3)
        table.setHorizontalHeaderLabels(["Statut", "Date_Modif_Statut", "Utilisateur"])
        table.setSelectionBehavior(QAbstractItemView.SelectRows)
        table.setSelectionMode(QAbstractItemView.MultiSelection)
        for row_idx, h in enumerate(historiques):
            table.setItem(row_idx, 0, QTableWidgetItem(h.get("Statut", "")))
            table.setItem(row_idx, 1, QTableWidgetItem(str(h.get("Date_Modif_Statut", ""))))
            table.setItem(row_idx, 2, QTableWidgetItem(h.get("Utilisateur_Modif_Statut", "")))
            if row_idx % 2 == 0:
                for col_idx in range(3):
                    table.item(row_idx, col_idx).setBackground(Qt.lightGray)
        table.resizeColumnsToContents()
        table.horizontalHeader().setSectionResizeMode(QHeaderView.Stretch)
        layout.addWidget(table)

        dialog.exec_()

    # Méthode qui permet d'afficher l'historique des mouvements d'une palette sélectionnée
    def afficher_historique_mvt(self):
        selected_rows = [index.row() for index in self.table.selectionModel().selectedRows()]
        if not selected_rows:
            QMessageBox.warning(self, "Erreur", "Aucune ligne sélectionnée")
            return

        num_palette = self.table.item(selected_rows[0], 0).text()

        try:
            conn = get_connection()
            with conn.cursor() as cursor:
                cursor.execute("""
                    SELECT Date_Dernier_MVT, Zone
                    FROM MVT_Palette
                    WHERE NumPalette = %s
                    ORDER BY Date_Dernier_MVT DESC
                """, (num_palette,))
                historiques = cursor.fetchall()
            conn.close()
        except Exception as e:
            logging.error(f"Erreur lors de l'affichage de l'historique de mouvement: {e}")
            QMessageBox.critical(self, "Erreur", f"Erreur lors de l'affichage de l'historique :\n{e}")
            return

        dialog = QDialog(self)
        dialog.setWindowTitle(f"Historique de mouvements - Palette {num_palette}")
        dialog.setGeometry(350, 350, 600, 300)
        layout = QVBoxLayout(dialog)

        table = QTableWidget(len(historiques), 2)
        table.setHorizontalHeaderLabels(["Date mouvement", "Zone"])
        table.setSelectionBehavior(QAbstractItemView.SelectRows)
        table.setSelectionMode(QAbstractItemView.SingleSelection)
        for row_idx, h in enumerate(historiques):
            table.setItem(row_idx, 0, QTableWidgetItem(str(h.get("Date_Dernier_MVT", ""))))
            table.setItem(row_idx, 1, QTableWidgetItem(h.get("Zone", "")))
            if row_idx % 2 == 0:
                for col_idx in range(2):
                    table.item(row_idx, col_idx).setBackground(Qt.lightGray)
        table.resizeColumnsToContents()
        table.horizontalHeader().setSectionResizeMode(QHeaderView.Stretch)
        layout.addWidget(table)

        dialog.exec_()

# Fonction main pour lancer l'application
if __name__ == '__main__':
    app = QApplication(sys.argv)
    login = LoginDialog()
    if login.exec_() == QDialog.Accepted:
        username = login.get_username()
        role = login.get_role()
        window = StockApp(username, role)
        window.show()
        sys.exit(app.exec_())
