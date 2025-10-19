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
