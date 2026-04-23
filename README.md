# RPTU Search Engine – IS Project WS 2024/25

A web search engine built with Java (Tomcat), React, PostgreSQL, Docker and Kubernetes.

---

## Project Structure

```
Search-engine/
├── src/                         # Java Backend (Maven WAR)
│   └── main/
│       ├── java/
│       │   ├── CommandInterface/   # CLI search interface
│       │   ├── Crawler/            # Web crawler (BFS, multi-threaded)
│       │   ├── DB/                 # DBConnection (JDBC + PostgreSQL)
│       │   ├── Indexer/            # HTML parser, stemmer, stopwords
│       │   ├── Servlet/            # SearchServlet (JSON API)
│       │   └── Sheet2/             # PageRank, BM25, Classifier
│       ├── resources/              # Stopwords, dictionaries
│       └── webapp/                 # (Legacy) JSP/HTML frontend
├── frontend/                    # React Frontend
│   ├── src/
│   │   ├── components/          # SearchPage, ResultsPage
│   │   ├── utils/               # queryParser.js
│   │   ├── App.jsx
│   │   └── index.js
│   ├── Dockerfile               # nginx-based container
│   └── nginx.conf               # Proxy /is-project/ → backend
├── k8s/                         # Kubernetes manifests
│   ├── namespace.yaml
│   ├── postgres-deployment.yaml
│   ├── backend-deployment.yaml
│   ├── frontend-deployment.yaml
│   ├── ingress.yaml
│   └── hpa.yaml                 # HorizontalPodAutoscaler
├── Dockerfile                   # Multi-stage: React build → WAR → Tomcat
├── docker-compose.yml           # Local development
├── deploy.sh                    # Build + deploy to Kubernetes
├── .env                         # Local secrets (not committed)
└── pom.xml
```

---

## Quick Start – Local Development

### Option A: Docker Compose (recommended)

```bash
# 1. Clone & enter the project
cd Search-engine

# 2. Start everything (PostgreSQL + Backend + Frontend)
docker-compose up --build

# 3. Open browser
#    React UI  →  http://localhost:3000
#    Java API  →  http://localhost:8080/is-project/search
```

### Option B: Without Docker

```bash
# 1. Start PostgreSQL locally (port 5432, DB: IS-Project, user: postgres, pw: 9157)

# 2. Build & run Java backend
mvn package -DskipTests
# Deploy target/is-project-1.0-SNAPSHOT.war to Tomcat

# 3. Start React frontend
cd frontend
npm install
npm start   # → http://localhost:3000
```

---

## Kubernetes Deployment

```bash
# Prerequisites: kubectl configured, Docker running

# 1. Build & push images (set REGISTRY in deploy.sh)
#    OR for local minikube: eval $(minikube docker-env)

# 2. Deploy everything
chmod +x deploy.sh
./deploy.sh

# 3. Watch pods
kubectl get pods -n search-engine -w
```

### Apply individual manifests

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml
```

---

## Search Query Syntax

| Syntax | Bedeutung |
|---|---|
| `database systems` | Disjunktiv – mindestens eines der Wörter |
| `"database"` | Konjunktiv – muss enthalten sein |
| `site:cs.rptu.de` | Nur Ergebnisse von dieser Domain |
| `"database" systems site:rptu.de` | Kombinierbar |

---

## API – JSON Interface

```
GET /is-project/search?query=<JSON>&k=<int>
```

**Query JSON Example:**
```json
{
  "conjuctiveSearchTerms": ["database"],
  "disjunctiveSearchTerms": ["systems", "course"],
  "domainSiteTerms": ["cs.rptu.de"],
  "scoreOption": "BM25",
  "languages": ["English", "German"]
}
```

**Response:**
```json
{
  "resultList": [
    { "rank": 1, "url": "https://...", "score": 1.234 }
  ],
  "query": { "k": 20, "query": "database systems" },
  "stat": [{ "term": "database", "df": 42 }],
  "cw": 10000
}
```

**Scoring options:**  `tfidf` | `BM25`

---

## Features Implemented

| Sheet | Feature | Status |
|---|---|---|
| 1 | Indexer (HTML parsing, stemming, stopwords) | ✅ |
| 1 | Crawler (BFS, multi-threaded, rate-limited) | ✅ |
| 1 | TF-IDF scoring | ✅ |
| 1 | Conjunctive & Disjunctive query modes | ✅ |
| 1 | CLI interface | ✅ |
| 1 | HTML UI + JSON Servlet | ✅ |
| 2 | PageRank (power iteration, convergence) | ✅ |
| 2 | BM25 scoring | ✅ |
| 2 | Combined score (BM25 + PageRank) | ✅ |
| 2 | Language detection (EN/DE) | ✅ |
| 2 | Spell correction (Levenshtein) | ✅ |
| –  | React frontend | ✅ |
| –  | Docker + docker-compose | ✅ |
| –  | Kubernetes (k8s/) + HPA | ✅ |

---

## Environment Variables (Docker/K8s)

| Variable | Default | Beschreibung |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL Host |
| `DB_PORT` | `5432` | PostgreSQL Port |
| `DB_NAME` | `IS-Project` | Datenbankname |
| `DB_USER` | `postgres` | Datenbankbenutzer |
| `DB_PASSWORD` | `9157` | Passwort |
