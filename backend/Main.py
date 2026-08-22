from fastapi import FastAPI, UploadFile, Form, File, status, HTTPException
from pydantic import BaseModel, EmailStr # ADDED: Required for data validation
import psycopg2
from psycopg2.extras import RealDictCursor
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

# Global temporary dictionary to track OTPs in laptop memory without Redis
# Shape will be: {"user_email@gmail.com": {"code": "123456", "expires_at": 1718293849}}
otp_storage = {}
#_______________________________________________________________________________________________________________________

class OtpVerificationRequest(BaseModel):
    email: EmailStr
    otp_code: str
#_______________________________________________________________________________________________________________________

class UserLogin(BaseModel):
    email: EmailStr
    password: str
#_______________________________________________________________________________________________________________________

app = FastAPI()
#_______________________________________________________________________________________________________________________

def send_otp_email(receiver_email: str, otp_code: str):
    # 1. Set up your sender credentials
    sender_email = "your_project_email@gmail.com"
    sender_password = "your_app_password" # ⚠️ This is NOT your normal login password!

    # 2. Construct the message envelope
    message = MIMEMultipart()
    message["From"] = sender_email
    message["To"] = receiver_email
    message["Subject"] = "Karya Account Verification Code"

    body = f"Hello! Your account confirmation code is: {otp_code}. It will expire in 5 minutes."
    message.attach(MIMEText(body, "plain"))

    try:
        # 3. Connect to Gmail's secure automated transmission pipeline (SMTP server)
        server = smtplib.SMTP("://gmail.com", 587)
        server.starttls() # Encrypt the connection line so hackers can't see passwords
        server.login(sender_email, sender_password)
        
        # 4. Fire the email across the internet!
        server.sendmail(sender_email, receiver_email, message.as_string())
        server.quit()
        print(f"OTP Email sent successfully to {receiver_email}!")
    except Exception as e:
        print(f"Failed to send email: {e}")
#_______________________________________________________________________________________________________________________

def get_db_connection(): # Single source of truth for connecting python to local postgreSQL database
    conn = psycopg2.connect(
        host="localhost",
        database="postgres",
        user="postgres",
        password="jay2592003"
    )
    return conn
#_______________________________________________________________________________________________________________________

# This class mirrors your Android user registration payloads perfectly
class UserRegisterRequest(BaseModel):
    name: str
    email: EmailStr # ADDED: Automatically validates correct email formats (e.g. rejects "hello@com")
    password: str
#_______________________________________________________________________________________________________________________

@app.get("/get-tasks") 
def read_tasks():
    connection = get_db_connection()
    cursor = connection.cursor(cursor_factory=RealDictCursor)
    cursor.execute("SELECT * FROM tasks WHERE task_id = 1;")
    tasks = cursor.fetchall()
    cursor.close()
    connection.close()
    return {"status": "success", "data": tasks}
#_______________________________________________________________________________________________________________________

@app.get("/about-karya")
def read_info():
    return {"company": "Karya", "mission": "Ethical data curation for AI models"}
#_______________________________________________________________________________________________________________________

@app.post("/login")
def login_user(login_data: UserLogin):
    connection = get_db_connection()
    cursor = connection.cursor()
    try:
        query = "SELECT id, name, email, password_hash, is_verified FROM users WHERE email = %s"
        cursor.execute(query,(login_data.email,))
        user_data= cursor.fetchone()
        if user_data is None:
            connection.rollback()
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="This email is not registered. Sign in instead"
            )
        id, name,looked_up_password,verification_status = (user_data[0],user_data[1],user_data[3],user_data[4])
        if verification_status == False:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Your account has not been verified, redirecting you to OTP screen to verify your email"
            )
        if login_data.password == looked_up_password:
            return {"status": "success", "user": {
                "id" : id,
                "name" : name,
            } }
        else:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="The password is incorrect"
            )
    except HTTPException:
        raise
    except Exception as e:
        print(f"SYSTEM CRASH LOG: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An unexpected server error occured during login."
        )
    finally:
        cursor.close()
        connection.close()
#_______________________________________________________________________________________________________________________

@app.post("/register", status_code=status.HTTP_201_CREATED)
def register_user(user_data: UserRegisterRequest):
    print(f"Incoming registration request: {user_data.name}, {user_data.email}")
    
    connection = get_db_connection()
    cursor = connection.cursor()

    try:

        check_query = "SELECT email FROM users WHERE email = %s"
        cursor.execute(check_query, (user_data.email,))
        email = cursor.fetchone()
        if email is not None:
            connection.rollback()
            cursor.close()
            connection.close()
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail= "This email is already in use. Please log in instead."
                )
        # Secure parameterized query prevents SQL injection attacks
        query = "INSERT INTO users (name, email, password_hash) VALUES (%s, %s, %s);"
        cursor.execute(query, (user_data.name, user_data.email, user_data.password))
        
        # FIXED: Lock the new user row permanently into your storage files
        connection.commit()
         # 2. Generate a random 6-digit verification code string
        generated_otp = str(random.randint(100000, 999999))
        
        # 3. Calculate expiration timestamp (Current epoch time + 300 seconds)
        five_minutes_in_seconds = 300
        expiration_time = time.time() + five_minutes_in_seconds
        
        # 4. Save this data envelope into our server's RAM dictionary index
        otp_storage[user_data.email] = {
            "code": generated_otp,
            "expires_at": expiration_time
        }
    
        # 5. Print it to terminal (and eventually fire your automation email script!)
        print(f"--- SECURITY LOG: Sent OTP {generated_otp} to {user_data.email}. Expiring in 5 mins. ---")
        # send_otp_email(user_data.email, generated_otp)
        # 3. Fire and forget the automation script!
        send_otp_email(receiver_email=user_data.email, otp_code=generated_otp)
    
        return {"status": "success", "message": "User registered. Verification OTP sent!"}
        
    except HTTPException:
        # If it's our own custom validation exception, re-raise it so FastAPI passes it to the phone
        raise
    except psycopg2.errors.UniqueViolation:
        connection.rollback()
        raise HTTPException(status_code=400, detail="Email is already registered!")
    except Exception as e:
        connection.rollback()
        raise HTTPException(status_code=500, detail=f"Database execution error: {str(e)}")
    finally:
        # 3. FIXED: Master layout wrapper ensures every single execution wire shuts down cleanly!
        cursor.close()
        connection.close()
#_______________________________________________________________________________________________________________________

@app.post("/verify-otp")
def verify_user_otp(payload: OtpVerificationRequest):
    email = payload.email
    user_submitted_code = payload.otp_code
    
    # Check 1: Does this email even have an active validation sequence inside memory?
    if email not in otp_storage:
        raise HTTPException(status_code=400, detail="No active verification session found!")
        
    session_data = otp_storage[email]
    
    # Check 2: Has the clock run out? (Compare current system time to expiration mark)
    if time.time() > session_data["expires_at"]:
        del otp_storage[email] # Garbage collect the dead entry out of RAM
        raise HTTPException(status_code=400, detail="OTP has expired! Please register again.")
        
    # Check 3: Do the code digits match?
    if session_data["code"] != user_submitted_code:
        raise HTTPException(status_code=400, detail="Incorrect verification code!")
        
    # Success! Clean out the memory locker and flip the verification row in PostgreSQL
    del otp_storage[email]
    
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("UPDATE users SET is_verified = TRUE WHERE email = %s;", (email,))
    conn.commit()
    cursor.close()
    conn.close()
    
    return {"status": "success", "message": "Account verified successfully!"}
#_______________________________________________________________________________________________________________________

@app.post("/submit-task")
def receive_worker_submission(payload: dict):
    t_id = payload.get("task_id")
    worker = payload.get("worker_name")
    text_data = payload.get("recorded_text")

    # FIXED: Replaced repetitive connection code with your standardized helper function!
    conn = get_db_connection()
    cursor = conn.cursor()

    insert_query = """
    INSERT INTO submissions (task_id, worker_name, recorded_text)
    VALUES (%s, %s, %s);
    """
    cursor.execute(insert_query, (t_id, worker, text_data))
    conn.commit()
    cursor.close()
    conn.close()

    return {"status": "success", "message": f"Data saved securely for {worker}!"}
#_______________________________________________________________________________________________________________________

@app.post("/upload-audio")
def receive_audio(worker_name: str = Form(),email: EmailStr = Form(),user_id: str = Form(), recorded_text: str = Form(), audio_file: UploadFile = File()):
    raw_bytes = audio_file.file.read()
    unique_filename = f"{worker_name}_voice.mp4"

    with open(unique_filename, "wb") as buffer: 
        buffer.write(raw_bytes)

    conn = get_db_connection()
    cursor = conn.cursor()

    insert_query = """
    INSERT INTO submissions (task_id, worker_name, recorded_text)
    VALUES (%s, %s, %s);
    """
    cursor.execute(insert_query, (102, worker_name, f"Audio File Saved As: {recorded_text}"))

    conn.commit()
    cursor.close()
    conn.close()
    print(f"Audio received from {worker_name}!")
    return {"status": "success"}
