# 🚀 DevOps Environment Setup 

This script sets up a full DevOps environment with:
- Java, Jenkins, Docker, SonarQube
- AWS CLI, Terraform, Helm
- Kubernetes (kubeadm), ArgoCD
- Security tools: Trivy
- Optional: Minikube
-Others
---

## 🛠️ Run This Script

```bash
chmod +x setup.sh
./setup.sh
```

> 📁 Make sure you're running as a user with `sudo` privileges.

---

## 📦 Installing Resources

### ☕ Java & Jenkins

```bash
#Java installation
sudo apt update -y
sudo apt install openjdk-17-jre openjdk-17-jdk -y
java --version
# Jenkins installation
curl -fsSL https://pkg.jenkins.io/debian/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update -y
sudo apt-get install jenkins -y
```
You can use below command to get Jenkins Admin Password

```bash
systemctl status jenkins
```

### 🐳 Installing Docker & Permissions

```bash
sudo apt install docker.io -y
```
Change Permissions to Jenkins
```bash
sudo usermod -aG docker jenkins # use when you integrate with jenkins
```
Change Permissions to Docker
```bash
sudo usermod -aG docker ubuntu
sudo systemctl restart docker
sudo chmod 777 /var/run/docker.sock #optional
```

---

## 🧪 SonarQube (Docker)

```bash
docker run -d --name sonar -p 9000:9000 sonarqube:lts-community
```
## Nexus (Docker)

```bash
docker run -d \
  --name nexus \
  -p 8081:8081 \
  -v nexus-data:/nexus-data \
  sonatype/nexus3
```
Default admin login:

Username: admin

Password: (found in /nexus-data/admin.password inside the container)
```bash
docker exec -it nexus cat /nexus-data/admin.password
```
Or Use Docker Compose (Recommended)

Create a file named docker-compose.yml:

```bash
version: '3'
services:
  nexus:
    image: sonatype/nexus3
    container_name: nexus
    ports:
      - "8081:8081"
    volumes:
      - nexus-data:/nexus-data
    restart: unless-stopped

volumes:
  nexus-data:
```
Then start it with:
```bash
docker-compose up -d
```
---

## ☁️ AWS CLI

```bash
#install aws cli
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt install unzip -y
unzip awscliv2.zip
sudo ./aws/install
```
To Configure AWS ( You might need aws user creds with necessary permissions)
```bash
aws configure
```

---

## ☸️ Installing Kubernetes Stack

### Install `kubectl`, `eksctl`, `containerd`, and dependencies

```bash
# Kernel modules and sysctl
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

sudo modprobe overlay
sudo modprobe br_netfilter

cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF

sudo sysctl --system
```

### Install Kubernetes tools

```bash
sudo apt-get install -y containerd curl apt-transport-https ca-certificates
containerd config default | sudo tee /etc/containerd/config.toml
sudo systemctl restart containerd

# Install kubeadm, kubelet, kubectl
KUBE_LATEST=$(curl -sL https://dl.k8s.io/release/stable.txt | awk 'BEGIN{FS="."}{printf "%s.%s", $1, $2}')
curl -fsSL https://pkgs.k8s.io/core:/stable:/${KUBE_LATEST}/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/${KUBE_LATEST}/deb/ /" | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
```

---

## 🧭 Installing Argo CD On K8s

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```
Patch service to LoadBalancer
```bash
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "LoadBalancer"}}'
```
Or Port Forward
```bash
kubectl port-forward svc/argocd-server -n argocd 9000:80 --address 0.0.0.0 > /dev/null & 
```
### 🔑 Get Argo CD Initial Admin Password

```bash
argocd admin initial-password -n argocd
```
OR
```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo
```
To log in argocd server from terminal
```bash
argocd login <ARGOCD_SERVER>
```
Change the password using the command
```bash
argocd account update-password
```
Example app yaml
```bash
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: guestbook
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/argoproj/argocd-example-apps.git
    targetRevision: HEAD
    path: guestbook
  destination:
    server: https://kubernetes.default.svc
    namespace: guestbook
```
(Optional) If Image Is From Github GHCR Repo You Need To Create Secrect To Prevent Image Pull Error
```bash
kubectl create secret docker-registry github-container-registry \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_TOKEN \
  --docker-email=YOUR_EMAIL \
  -n your-namespace
```
---

## 🔐 Installing Security Tools

### Install Trivy

```bash
#install trivy
sudo apt-get install -y wget gnupg lsb-release
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main | sudo tee /etc/apt/sources.list.d/trivy.list
sudo apt update
sudo apt install trivy -y
```

---

## 🔧 IAC Tools

### Terraform

```bash
#install terraform
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install terraform -y
```
### Or you can use OpenTofu 
cmd will add...

### Helm

```bash
sudo snap install helm --classic
```
### Sealed Secrect
Install kubeseal CLI (Local Machine or on Bastion Host)

🔽 Linux:
```bash
wget https://github.com/bitnami-labs/sealed-secrets/releases/latest/download/kubeseal-linux-amd64 -O kubeseal
chmod +x kubeseal
sudo mv kubeseal /usr/local/bin/
```
Install Sealed Secrets Controller using Helm
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
helm install sealed-secrets bitnami/sealed-secrets
```
Optional: Set the namespace
```bash
helm install sealed-secrets bitnami/sealed-secrets --namespace kube-system
```
Fetch the Public Certificate

This is required to encrypt secrets offline with kubeseal.
```bash
kubeseal --fetch-cert \
  --controller-name=sealed-secrets-controller \
  --controller-namespace=kube-system > pub-cert.pem
```
Create and Seal a Secret

🔧 Create a Kubernetes secret (On your local machine):

```bash
kubectl create secret generic mysecret \
  --from-literal=password=mypassword \
  --dry-run=client -o yaml > mysecret.yaml
```
🔐 Seal it:

```bash
kubeseal --format=yaml < mysecret.yaml > sealedsecret.yaml
```
You can now store sealedsecret.yaml in Git safely.

Apply the Sealed Secret to the Cluster
```bash
kubectl apply -f sealedsecret.yaml
```
Integration with Vault will add soon
---

## 🧩 Kubernetes Cluster Set Up

With Kubeadm

ON-BOTH
------------------

```bash
ip route | grep default | awk '{ print $9 }'

PRIMARY_IP=
POD_CIDR=10.244.0.0/16
SERVICE_CIDR=10.96.0.0/1

sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl

cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

sudo modprobe overlay
sudo modprobe br_netfilter

cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF

sudo sysctl --system

sudo apt-get install -y containerd
sudo mkdir -p /etc/containerd
containerd config default | sed 's/SystemdCgroup = false/SystemdCgroup = true/' | sudo tee /etc/containerd/config.toml
sudo systemctl restart containerd

KUBE_LATEST=$(curl -L -s https://dl.k8s.io/release/stable.txt | awk 'BEGIN { FS="." } { printf "%s.%s", $1, $2 }')
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://pkgs.k8s.io/core:/stable:/${KUBE_LATEST}/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/${KUBE_LATEST}/deb/ /" | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl

sudo crictl config \
    --set runtime-endpoint=unix:///run/containerd/containerd.sock \
    --set image-endpoint=unix:///run/containerd/containerd.sock

cat <<EOF | sudo tee /etc/default/kubelet
KUBELET_EXTRA_ARGS='--node-ip ${PRIMARY_IP}'
EOF
```

ON-MASTER
-----------------

```bash

POD_CIDR=10.244.0.0/16
SERVICE_CIDR=10.96.0.0/16
sudo kubeadm init --pod-network-cidr $POD_CIDR --service-cidr $SERVICE_CIDR --apiserver-advertise-address $PRIMARY_IP
mkdir ~/.kube
sudo cp /etc/kubernetes/admin.conf ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
chmod 600 ~/.kube/config

kubectl get pods -n kube-system
kubectl apply -f "https://github.com/weaveworks/weave/releases/download/v2.8.1/weave-daemonset-k8s-1.11.yaml"
kubectl get pods -n kube-system
```

ON-NODE
----------------

```bash

sudo -i
paste join cmd
```

---

## 📋 Notes

- Make sure to paste `kubeadm join` output from the master into your worker node.
- Replace `<your-primary-ip>` with the result of: `ip route | grep default | awk '{ print $9 }'`

---
## Minikube Set Up

```bash
wget https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
chmod +x minikube-linux-amd64
sudo mv minikube-linux-amd64 /usr/local/bin/minikube
```
To Start Minikube
```bash
#minikube start --driver=<driver> --cpus=<number> --memory=<amount>
minikube start --driver=docker --cpus=2 --memory=4096
```

---


## 📎 License

MIT – Free to use, modify, and distribute.
