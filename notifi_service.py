import smtplib
from email.message import EmailMessage
from pymongo import MongoClient

mongo_uri = "mongodb+srv:***"
email_user = "***"
db_name = "***"
email_pass = "***"
sender_email = "***"

MONGO_URI = mongo_uri
DB_NAME = db_name
EMAIL_HOST = "smtp.gmail.com"
EMAIL_PORT = 587
EMAIL_USER = email_user
EMAIL_PASS = email_pass


client = MongoClient(MONGO_URI)
db = client[DB_NAME]

def get_template(template_id, service_type="email"):
    return db.templates.find_one({"template_id": template_id, "service_type": service_type})

def fill_placeholders(text, args):
    for key, value in args.items():
        text = text.replace(f"{{{{{key}}}}}", str(value))
    return text


def send_email(to, subject, body):
    msg = EmailMessage()
    msg["From"] = EMAIL_USER
    msg["To"] = to
    msg["Subject"] = subject
    msg.set_content(body)

    with smtplib.SMTP(EMAIL_HOST, EMAIL_PORT) as server:
        server.starttls()
        server.login(EMAIL_USER, EMAIL_PASS)
        server.send_message(msg)
    print(f"Email sent to {to}")


def notify(to, notification_type, template_id, arguments):
    template = get_template(template_id, notification_type)
    if not template:
        print("Template not found")
        return

    subject = fill_placeholders(template["subject"], arguments)
    body = fill_placeholders(template["body"], arguments)

    if notification_type == "email":
        send_email(to, subject, body)
    else:
        print(f"Notification type '{notification_type}' not supported yet.")


notify(
    to=sender_email,
    notification_type="email",
    template_id=101,
    arguments={
        "name": "John Doe",
        "account_id": "12345"
    }
)


{
  "template_id": 101,
  "service_type": "email",
  "subject": "Hello {{name}}, Welcome!",
  "body": "Your account {{account_id}} has been created successfully."
}