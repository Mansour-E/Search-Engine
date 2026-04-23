#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy.sh  –  Baut Docker-Images und deployt alles auf Kubernetes
#
# Voraussetzungen:
#   - Docker läuft
#   - kubectl ist konfiguriert (z.B. minikube, k3s, oder echtes Cluster)
#   - (Optional) eine Container Registry  →  REGISTRY Variable anpassen
#
# Usage:
#   chmod +x deploy.sh
#   ./deploy.sh
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

REGISTRY=""          # z.B. "docker.io/deinname"  – leer lassen für lokales Image
BACKEND_IMAGE="search-engine-backend:latest"
FRONTEND_IMAGE="search-engine-frontend:latest"

echo "══════════════════════════════════════════"
echo " RPTU Search Engine  –  Deploy Script"
echo "══════════════════════════════════════════"

# 1) Namespace anlegen (idempotent)
echo "▶ Namespace..."
kubectl apply -f k8s/namespace.yaml

# 2) Docker-Images bauen
echo "▶ Building backend image..."
docker build -t "${REGISTRY:+$REGISTRY/}${BACKEND_IMAGE}" .

echo "▶ Building frontend image..."
docker build -t "${REGISTRY:+$REGISTRY/}${FRONTEND_IMAGE}" ./frontend

# 3) Optional: Images pushen (nur wenn REGISTRY gesetzt)
if [ -n "$REGISTRY" ]; then
  echo "▶ Pushing images to $REGISTRY ..."
  docker push "${REGISTRY}/${BACKEND_IMAGE}"
  docker push "${REGISTRY}/${FRONTEND_IMAGE}"
fi

# 4) Kubernetes Ressourcen anwenden
echo "▶ Deploying PostgreSQL..."
kubectl apply -f k8s/postgres-deployment.yaml

echo "▶ Waiting for PostgreSQL to be ready..."
kubectl rollout status deployment/postgres -n search-engine --timeout=120s

echo "▶ Deploying Backend..."
kubectl apply -f k8s/backend-deployment.yaml

echo "▶ Deploying Frontend..."
kubectl apply -f k8s/frontend-deployment.yaml

echo "▶ Applying Ingress..."
kubectl apply -f k8s/ingress.yaml

echo "▶ Applying HPA (Autoscaler)..."
kubectl apply -f k8s/hpa.yaml

echo ""
echo "══════════════════════════════════════════"
echo " ✓ Deployment complete!"
echo ""
echo " Pods:"
kubectl get pods -n search-engine
echo ""
echo " Services:"
kubectl get svc -n search-engine
echo ""
echo " Ingress:"
kubectl get ingress -n search-engine
echo "══════════════════════════════════════════"
