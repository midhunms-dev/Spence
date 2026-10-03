<div align="center"><img width="200" height="200" alt="ChatGPT Image Oct 3, 2026, 12_03_28 PM" src="https://github.com/user-attachments/assets/be440c70-4e8d-4446-90b1-dbf175457419" />

  <h1> Spence </h1>

 <p><b>**Smart Expense & Credit Card Manager** </b></p>
 </div>

A privacy-first, powerful personal finance and expense tracking companion for Android.

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Version](https://img.shields.io/badge/Version-v1.0.0-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)
![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20%26%20Secure-brightgreen.svg)

---

## Table of Contents
- [Overview](#overview)
- [Key Features](#key-features)
  - [Liquid Balance & Cash Flow](#liquid-balance--cash-flow)
  - [Advanced Credit Card Management](#advanced-credit-card-management)
  - [Multi-Account & Cash Tracking](#multi-account--cash-tracking)
  - [Intelligent EMI & Loan Engine](#intelligent-emi--loan-engine)
  - [Analytics & Reports](#analytics--reports)
  - [Data Backup & Privacy](#data-backup--privacy)
- [Screenshots](#screenshots)
- [Installation and Setup](#installation-and-setup)
  - [Prerequisites](#prerequisites)
  - [Building from Source](#building-from-source)
- [Tech Stack](#tech-stack)
- [Support the Project](#support-the-project)
- [Legal Disclaimer & Terms of Use](#legal-disclaimer--terms-of-use)
- [License](#license)

---

## Overview

Spence is designed to take the chaos out of managing daily finances, bank accounts, multiple credit card billing cycles, and ongoing loans/EMIs. Built with an intuitive, modern interface, Spence gives you total visibility over your true liquid wealth without sending your sensitive financial data to any third-party server.

---

## Key Features

### Liquid Balance & Cash Flow
- Total Liquid Balance at a Glance: Instantly view actual spendable money (Bank Accounts + Physical Cash).
- Monthly Summary: Real-time visibility into Monthly Spend, Available Credit limits, and Total Loan Liabilities.

### Advanced Credit Card Management
- Billing Cycle Tracking: Track spends aligned strictly with your statement cycle dates (e.g., 16th Sep – 15th Oct), rather than just calendar months.
- Credit Limit & Utilization: Keep your credit health in check with real-time utilization meters (e.g., Healthy < 30%).
- One-Tap Bill Settlement: Dedicated "Pay CC Bill" shortcuts to record and reconcile payments.

### Multi-Account & Cash Tracking
- Organize balances across multiple Bank Accounts and dedicated Cash in Hand ledgers.
- Keep tabs on individual account balances with clear transaction histories.

### Intelligent EMI & Loan Engine
- Dedicated EMI Tracking: Monitor running EMIs, remaining tenure, paid installments, and pending principal.
- Smart Linking:
  - Credit Card Clubbed EMIs: Automatically adjust remaining credit limit for active installments.
  - Bank Deductions: Auto-cut or record recurring auto-debit payments directly from linked savings accounts.
- Due Reminders: Smart reminders triggered prior to the due date to prevent missed payments.

### Analytics & Reports
- Dual View Filtering: Toggle between standard Calendar Month views and individual Card Billing Cycle views.
- Categorized Spending: Visual breakdowns of expenditure patterns across shopping, food, bills, and utilities.

### Data Backup & Customization
- 100% Private & Local-First: Your data stays on your device.
- Backup & Restore: Seamless manual and automated scheduled backups (Daily, Weekly, Monthly) via Google Drive and local storage.
- Theme Options: Seamless switching between System Default, Dark Mode, and Light Mode.

---

## Screenshots

| Overview & Balance | EMI & Loans | Cards & Accounts | Reports & Analytics |
| :---: | :---: | :---: | :---: |
<img width="200" height="444" alt="1000717673" src="https://github.com/user-attachments/assets/fdea6990-70a2-4629-bfad-6c4a0d8d5125" />
<img width="200" height="444" alt="1000717672" src="https://github.com/user-attachments/assets/b3d0bb45-2ae1-4014-b613-328bde00fd8c" />
<img width="200" height="444" alt="1000717678" src="https://github.com/user-attachments/assets/48ab9281-415b-42c2-9345-3f6a655b57e7" />
<img width="200" height="444" alt="1000717677" src="https://github.com/user-attachments/assets/933d478a-a646-4828-b6d7-7f861bd4b82d" />
<img width="200" height="444" alt="1000717676" src="https://github.com/user-attachments/assets/fbeab08d-c189-42e9-9971-0ba14079789b" />
<img width="200" height="444" alt="1000717675" src="https://github.com/user-attachments/assets/95b9a422-bbcf-4b50-a01b-c10f6420cbcb" />
<img width="200" height="444" alt="1000717674" src="https://github.com/user-attachments/assets/4c8e01d0-916e-4967-bcbb-91ba53aef40f" />


---

## Installation and Setup

### Prerequisites
- Android Studio Iguana (or newer)
- JDK 17 or higher
- Android SDK (API Level 26 minimum recommended)

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/spence.git
   cd spence
   ```

2. Open the project:
   - Launch Android Studio.
   - Select "Open an Existing Project" and choose the cloned `spence` folder.

3. Sync Dependencies:
   - Allow Gradle to download required dependencies and sync the project files.

4. Run the App:
   - Connect your Android device via USB debugging or start an Android Emulator.
   - Click the green Run button (`Shift + F10`) in Android Studio.

---

## Tech Stack

- Platform: Android
- Language: Kotlin / Java
- Architecture: Clean Architecture / MVVM
- Local Database: Room Database / SQLite
- UI: Jetpack Compose / Material 3 Design
- Backup: Google Drive REST API integration & Local Storage SAF

---
## Download
<a href="https://drive.google.com/file/d/1PRSLetEOlJH1NxJBu5a5UNw1NGVnymbz/view?usp=drive_link"> Click here to <b>DOWNLOAD</b></a>
___
## Support the Project

If you find Spence helpful and want to support its ongoing development:

- Star the Repository: If this project helped you, give it a star on GitHub!
- Report Issues: Found a bug or have a suggestion? Open an Issue on GitHub.
- Contribute: Pull requests are always welcome! Feel free to fork the repository and submit improvements.

---

## Legal Disclaimer & Terms of Use

### 1. General Financial Information
Spence is designed strictly as a personal budgeting and expense tracking utility. It is not an automated banking system, credit repair service, or financial advisory tool. Any calculations, interest estimates, EMI schedules, or credit limit representations provided by the app are for informational purposes only.

### 2. User Responsibility
Users are solely responsible for verifying the accuracy of all manual entries, bank balances, loan details, and credit card statement dates. The developers do not accept liability for discrepancies between the app's records and actual bank statements, financial penalties, late fees, or missed payments.

### 3. Data Privacy & Security
Spence operates on a local-first architecture. We do not host central servers storing your personal credentials, account numbers, or transaction logs. Backups uploaded to Google Drive or local storage are governed by the user's personal cloud configuration and device security settings.

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

Developed with love by Midhun
