# 📱 Weight Tracker App

## 📌 Overview
The **Weight Tracker App** is a simple and intuitive Android app that helps users record and monitor their weight over time.  
It’s built using **Kotlin** and **Jetpack Compose**, with a focus on clean design, smooth performance, and local data security.  
Users can set a 4-digit PIN to protect their data, log daily weight entries, and even get gentle reminders to stay consistent.

---

## ✨ Features
- 🔐 **Secure PIN Login:** Protects access to the app with a custom 4-digit PIN.  
- ⚖️ **Daily Weight Tracking:** Add, edit, and delete entries to stay on top of your progress.  
- 📊 **Stats at a Glance:** See your recent trends and average weight easily.  
- 💾 **Offline Storage:** Uses **Room (SQLite)** to save your data directly on your device.  
- 🔔 **Daily Notifications:** Optional reminders help keep you consistent with your logging routine.  
- 🔄 **Unit Toggle:** Switch between pounds and kilograms anytime.

---

## ⚙️ Development Details
This project follows a modular and clean architecture, separating the UI, logic, and data layers.  
I used **Kotlin Flow** to handle real-time updates in the interface and kept the design responsive and easy to navigate.  
Testing was done on both physical Android devices and the emulator to make sure everything ran smoothly.

---

## 🚧 Challenges & Solutions
- 🧱 **Data Persistence:** Originally, data was stored in memory, but I migrated to a full **Room Database** for reliability.  
- 🔐 **Permission Handling:** I added logic to handle notification permissions gracefully, so the app works even if they’re denied.  
- ⚖️ **Unit Conversion:** Implemented a clean toggle system between **lbs** and **kg** without affecting stored data accuracy.

---

## 🎯 What I Learned
This project taught me a lot about **Android development best practices**, from secure local storage to responsive UI design.  
I became more confident using **Jetpack Compose**, **Room**, and **DataStore**, and learned how to combine functionality with a smooth user experience.  
Most importantly, I learned how to think like a user — building something simple, reliable, and genuinely useful.

---

## Professional Self-Assessment
Completing the Computer Science program at Southern New Hampshire University has greatly improved my technical skills and career outlook. My ePortfolio highlights my journey from basic programming to advanced software engineering projects, showing my ability to design, build, and secure computing solutions.
During the program, I built teamwork and communication skills and focused on problem-solving in fast-paced environments. Courses like CS 260: Data Structures and Algorithms taught me how to understand computing efficiency. CS 360: Mobile Architecture and Programming helped me improve my software design skills. DAD 220: Introduction to SQL and my work on the Room Database taught me about data management and security. This knowledge led to the creation of my Weight Tracker App.
My ePortfolio showcases a variety of skills through several projects. It includes a software engineering project that demonstrates the development of user-friendly applications, an algorithms and data structures project that emphasizes efficient solutions, and a database project focused on secure data storage. Together, these projects reflect my technical abilities and professionalism.
This program has prepared me to keep growing in cybersecurity and software development while strengthening my problem-solving and communication skills. The skills and experiences in my ePortfolio demonstrate my dedication to creating secure, innovative, and user-centered technology solutions.
