# 🤝 Contributing Guide

## ⚠️ Important Notice

Acest proiect este un **proiect academic de licență** și **nu acceptă contribuții externe** în acest moment. 

---

## 📋 Dacă ești Zaharia Iulian (Autorul)

Dacă ești autorul și vrei să lucrezi pe acest proiect, urmează aceste guidelines:

### 🔄 Git Workflow

1. **Clone repository-ul**
   ```bash
   git clone https://github.com/zaheudev/event-driven-payment-system.git
   cd demo
   ```

2. **Create feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   # sau
   git checkout -b bugfix/your-bug-name
   ```

3. **Commit changes**
   ```bash
   git add .
   git commit -m "feat: brief description" # Urmează Conventional Commits
   ```

4. **Push și open Pull Request**
   ```bash
   git push origin feature/your-feature-name
   ```

### 📝 Conventional Commits Format

Folosește formatul:
- `feat:` - Feature nou
- `fix:` - Bug fix
- `docs:` - Documentație
- `refactor:` - Refactoring code
- `test:` - Adăugare tests
- `chore:` - Build, dependencies

Exemple:
```
feat: add payment processor service
fix: resolve Kafka consumer offset issue
docs: update README with setup instructions
refactor: extract validation logic to separate class
test: add unit tests for PaymentService
chore: upgrade Spring Boot to 3.2.3
```

### ✅ Before Commit

1. **Build local**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Run tests**
   ```bash
   mvn test
   ```

3. **Check code quality**
   ```bash
   mvn checkstyle:check
   # sau folosește IDE inspections
   ```

### 🌿 Branch Naming Convention

- `main` - Production-ready code (stable)
- `develop` - Development branch (active development)
- `feature/*` - Noi feature-uri (din develop)
- `bugfix/*` - Bug fixes (din develop)
- `hotfix/*` - Urgent fixes pentru main

### 📦 Code Style

- Follow **Java Google Style Guide**
- Use Lombok pentru reducerea boilerplate
- Documentează public classes cu JavaDoc
- Maximum line length: 120 characters

---

## 🚫 What to NOT Do

❌ Nu modifica fișiere de configurare locale fără discuție  
❌ Nu commit-uiți `target/`, `.idea/`, sau alte fișiere IDE  
❌ Nu schimbi Java version-ul fără update Maven config  
❌ Nu adaugi dependențe mari fără justificare  
❌ Nu pushuiești direct pe `main` branch  

---

## 📞 Questions?

Contact autorul: Zaharia Iulian

---

**Last updated:** Februarie 2026

