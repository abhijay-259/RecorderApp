from builtins import str
import os

import firebase_admin
from firebase_admin import credentials
from firebase_admin import auth as firebase_auth
from fastapi import FastAPI, UploadFile, Form, File, status, HTTPException, Header, Depends
from pydantic import BaseModel # ADDED: Required for data validation
import psycopg2
from psycopg2.extras import RealDictCursor

cred_path = os.environ.get("FIREBASE_PRIVATE_KEY_PATH")
cred = credentials.Certificate(cred_path)
firebase_admin.initialize_app(cred)
#_______________________________________________________________________________________________________________________

class TaskPayload(BaseModel):
    task_id: int
    worker_name: str
    recorded_text: str
#_______________________________________________________________________________________________________________________

app = FastAPI()
db_password = os.environ.get("DB_PASSWORD")
#_______________________________________________________________________________________________________________________
def verify_firebase_token(authorization: str = Header(...)) -> str:
    # 1. The header arrives as "Bearer <token>" - split it apart
    if not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code = status.HTTP_401_UNAUTHORIZED,
            detail = "Missing or malformed authorization header"
        )

    id_token = authorization.removeprefix("Bearer ").strip()

    # 2. Ask Firebase to verify the token's signature and expiry
    try:
        decoded_token = firebase_auth.verify_id_token(id_token)
    except Exception as e:
        raise HTTPException(
            status_code = status.HTTP_401_UNAUTHORIZED,
            detail = f"Invalid or expired token"
        )

    # 3. If verification succeeded, decoded_token is a dict containing the user's real data
    return decoded_token["uid"]
#_______________________________________________________________________________________________________________________

def get_db_connection(): # Single source of truth for connecting python to local postgreSQL database
    conn = psycopg2.connect(
        host="localhost",
        database="postgres",
        user="postgres",
        password=db_password
    )
    return conn
#_______________________________________________________________________________________________________________________

@app.get("/get-tasks") 
def read_tasks(task_id: int):
    connection = get_db_connection()
    cursor = connection.cursor(cursor_factory=RealDictCursor)
    query = "SELECT * FROM tasks WHERE task_id = %s;"
    cursor.execute(query,(task_id,))
    tasks = cursor.fetchall()
    cursor.close()
    connection.close()
    return {"status": "success", "data": tasks}
#_______________________________________________________________________________________________________________________

@app.get("/about-karya")
def read_info():
    return {"company": "Karya", "mission": "Ethical data curation for AI models"}
#_______________________________________________________________________________________________________________________

@app.post("/submit-task", status_code=status.HTTP_202_ACCEPTED)
def receive_worker_submission(payload: TaskPayload, uid: str = Depends(verify_firebase_token)):
    # uid is now the verified Firebase user ID - trustworthy, not client_controlled
    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        insert_query = """
        INSERT INTO submissions (task_id, worker_name, recorded_text, firebase_uid)
        VALUES (%s, %s, %s, %s);
        """
        cursor.execute(insert_query, (payload.task_id, payload.worker_name, payload.recorded_text, uid))
        conn.commit()
        return {"status": "success", "message": f"Data saved securely for {payload.worker_name}!"}
    except Exception as e:
        conn.rollback()
        print(f"SYSTEM CRASH LOG {str(e)}")
        raise HTTPException(status_code=500, detail="Database execution error while saving submission.")
    finally:
        cursor.close()
        conn.close()
#_______________________________________________________________________________________________________________________

@app.post("/upload-audio")
def receive_audio(
    worker_name: str = Form(), 
    recorded_text: str = Form(), 
    audio_file: UploadFile = File(),
    uid: str = Depends(verify_firebase_token)
    ):
    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        raw_bytes = audio_file.file.read()
        unique_filename = os.path.basename(recorded_text)

        if not unique_filename.endswith(".mp4"):
            raise HTTPException(status_code=400, detail="Invalid file name")
        upload_dir = "uploads"
        os.makedirs(upload_dir, exist_ok=True)
        file_path = os.path.join(upload_dir, unique_filename)
        with open(file_path, "wb") as buffer: 
            buffer.write(raw_bytes)

        insert_query = """
        INSERT INTO submissions (task_id, worker_name, recorded_text, firebase_uid)
        VALUES (%s, %s, %s, %s);
        """
        cursor.execute(insert_query, (102, worker_name, f"Audio File Saved As: {recorded_text}", uid))

        conn.commit()
        print(f"Audio received from {worker_name}!")
        return {"status": "success"}
    except HTTPException:
        raise
    except Exception as e:
        if conn:
            conn.rollback()
        print(f"SYSTEM CRASH LOG {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error {str(e)}")
    finally:
        cursor.close()
        conn.close()
