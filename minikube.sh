#!/usr/bin/env bash
# install-minikube-ubuntu.sh
set -e

echo "📦 Updating apt"
sudo apt update -y

echo "🧰 Installing Docker"
sudo apt install -y docker.io
sudo usermod -aG docker "$USER"
newgrp docker || true   # reload group without logout

echo "🚀 Installing Minikube"
curl -Lo minikube https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
chmod +x minikube
sudo mv minikube /usr/local/bin/minikube

echo "🛠️ Installing kubectl"
curl -Lo kubectl "https://dl.k8s.io/release/$(curl -Ls https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/kubectl

echo "🐳 Starting Minikube with 2 CPU / 4 GB / 20 GB disk"
minikube start --driver=docker --cpus=2 --memory=4096 --disk-size=20g

echo "🔌 Enabling essential add-ons"
minikube addons enable ingress
minikube addons enable metrics-server

echo "✅ Done! Run:"
echo "  minikube status"
echo "  kubectl get nodes"
